package com.trade.gateway.filter;

import com.trade.common.config.TraceIdContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdContext.TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdContext.generateTraceId();
        }

        MDC.put(TraceIdContext.TRACE_ID_MDC_KEY, traceId);

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(TraceIdContext.TRACE_ID_HEADER, traceId)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signalType -> MDC.remove(TraceIdContext.TRACE_ID_MDC_KEY));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
