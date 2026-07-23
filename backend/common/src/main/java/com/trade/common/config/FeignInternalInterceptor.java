package com.trade.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignInternalInterceptor implements RequestInterceptor {

    public static final String INTERNAL_HEADER = "X-Internal-Service";
    public static final String INTERNAL_HEADER_VALUE = "true";

    @Override
    public void apply(RequestTemplate template) {
        template.header(INTERNAL_HEADER, INTERNAL_HEADER_VALUE);

        String traceId = TraceIdContext.getTraceId();
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TraceIdContext.TRACE_ID_HEADER, traceId);
        }
    }
}
