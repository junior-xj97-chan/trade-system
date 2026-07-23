package com.trade.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Gateway 全局鉴权过滤器
 * <p>
 * 功能：
 * 1. 校验请求 Header 中的 Token
 * 2. 将用户信息（userId）注入到请求中传递给下游服务
 * 3. 白名单路径放行（登录、注册、文档等）
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Token 存储前缀（与 UserService 保持一致）
     */
    private static final String TOKEN_PREFIX = "user:token:";

    /**
     * Token 有效期（2小时）
     */
    private static final Duration TOKEN_EXPIRE = Duration.ofHours(2);

    /**
     * 白名单路径（不需要鉴权）
     */
    private static final List<String> WHITE_LIST = List.of(
        "/api/user/login",           // 登录
        "/api/user/register",        // 注册
        "/api/user/sendCode",        // 发送验证码
        "/actuator",                 // Actuator 健康检查（内部探针，统一放行）
        "/doc.html",                 // Swagger 文档
        "/v3/api-docs",             // OpenAPI 文档
        "/swagger-ui",               // Swagger UI
        "/favicon.ico"              // 网站图标
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 0. 剥离外部请求中的内部服务请求头，防止伪造
        ServerHttpRequest.Builder requestBuilder = request.mutate();
        requestBuilder.headers(headers -> headers.remove("X-Internal-Service"));
        request = requestBuilder.build();
        exchange = exchange.mutate().request(request).build();

        // 1. 白名单放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取 Token
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            log.warn("【鉴权失败】路径={}，原因：未提供Token", path);
            return unauthorized(exchange.getResponse(), "请先登录");
        }

        // 3. 校验 Token（从 Redis 查询，使用同步方式避免响应式链问题）
        String tokenKey = TOKEN_PREFIX + token;
        log.info("【鉴权】路径={}, token={}", path, token);

        try {
            Object userIdObj = redisTemplate.opsForValue().get(tokenKey);

            if (userIdObj != null) {
                // ============ 有值：校验通过 ============
                Long userId;
                if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                } else if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else {
                    userId = Long.parseLong(userIdObj.toString());
                }

                // 注入 X-User-Id 到请求头
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId.toString())
                        .build();
                log.info("【鉴权成功】路径={}, userId={}", path, userId);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } else {
                // ============ 无值：Token无效或已过期 ============
                log.warn("【鉴权失败】路径={}，token={}，原因：Token无效或已过期", path, token);
                return unauthorized(exchange.getResponse(), "Token已过期，请重新登录");
            }
        } catch (Exception e) {
            log.error("【鉴权异常】路径={}, error={}", path, e.getMessage(), e);
            return unauthorized(exchange.getResponse(), "鉴权服务异常，请稍后重试");
        }
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求中提取 Token
     */
    private String extractToken(ServerHttpRequest request) {
        // 优先从 Header 中获取（去掉 "Bearer " 前缀，只保留原始 token 值）
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers != null && !headers.isEmpty()) {
            String authHeader = headers.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7).trim();
            }
            // Header 中直接是 token 值（无 Bearer 前缀）
            return authHeader;
        }

        // 其次从 Query 参数中获取
        return request.getQueryParams().getFirst("token");
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // 过滤器优先级，数值越小越先执行
        return -100;
    }
}
