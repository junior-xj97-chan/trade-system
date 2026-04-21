package com.trade.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
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
 * 4. Token 续期（每次访问自动延长过期时间）
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

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
        "/doc.html",                 // Swagger 文档
        "/v3/api-docs",               // OpenAPI 文档
        "/swagger-ui",               // Swagger UI
        "/favicon.ico"               // 网站图标
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

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

        // 3. 校验 Token（从 Redis 查询）- 异步操作
        String tokenKey = TOKEN_PREFIX + token;
        ReactiveValueOperations<String, Object> ops = redisTemplate.opsForValue();
        
        return ops.get(tokenKey)
            .flatMap(userIdObj -> {
                if (userIdObj == null) {
                    log.warn("【鉴权失败】路径={}，原因：Token无效或已过期", path);
                    return unauthorized(exchange.getResponse(), "Token已过期，请重新登录");
                }

                Long userId;
                if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else {
                    userId = Long.parseLong(userIdObj.toString());
                }

                // 4. Token 续期（每次访问自动延长过期时间）
                return ops.set(tokenKey, userIdObj, TOKEN_EXPIRE)
                    .then(Mono.defer(() -> {
                        // 5. 将 userId 注入到请求 Header 中，传递给下游服务
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id", userId.toString())
                                .build();
                        log.debug("【鉴权成功】路径={}, userId={}", path, userId);
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    }));
            })
            .onErrorResume(e -> {
                log.error("【鉴权异常】路径={}, error={}", path, e.getMessage());
                return unauthorized(exchange.getResponse(), "鉴权服务异常，请稍后重试");
            });
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
        // 优先从 Header 中获取
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers != null && !headers.isEmpty()) {
            String authHeader = headers.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // 其次从 Query 参数中获取
        String token = request.getQueryParams().getFirst("token");
        return token;
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // 过滤器优先级，数值越小越先执行
        return -100;
    }
}
