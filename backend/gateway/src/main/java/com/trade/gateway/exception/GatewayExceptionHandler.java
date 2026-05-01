package com.trade.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import reactor.core.publisher.Mono;

/**
 * Gateway专用异常处理器
 * 优先级最高，专门处理404 NoResourceFoundException
 * 避免被 common.GlobalExceptionHandler 的 RuntimeException 处理器拦截
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final String HTML_404 = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>404 - 页面未找到</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #333;
                    }
                    .container {
                        background: white;
                        border-radius: 20px;
                        padding: 60px 40px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        text-align: center;
                        max-width: 500px;
                        margin: 20px;
                    }
                    .error-code {
                        font-size: 120px;
                        font-weight: bold;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                        line-height: 1;
                        margin-bottom: 20px;
                    }
                    h1 { font-size: 28px; color: #333; margin-bottom: 15px; }
                    p { font-size: 16px; color: #666; margin-bottom: 30px; line-height: 1.6; }
                    .btn {
                        display: inline-block;
                        padding: 12px 30px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        text-decoration: none;
                        border-radius: 25px;
                        font-size: 16px;
                        transition: transform 0.3s;
                    }
                    .btn:hover { transform: translateY(-2px); }
                </style>
            </head>
            <body>
                <div class="container">
                    <div style="font-size: 80px; margin-bottom: 20px;">🔍</div>
                    <div class="error-code">404</div>
                    <h1>页面未找到</h1>
                    <p>抱歉，您访问的页面不存在。<br>请检查URL是否正确，或返回首页。</p>
                    <a href="/" class="btn">返回首页</a>
                </div>
            </body>
            </html>
            """;


    /**
     * 处理 404 NoResourceFoundException
     * 优先级高于 GlobalExceptionHandler.handleRuntimeException
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<String>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("【404资源未找到】{}", e.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_HTML)
                .body(HTML_404));
    }
}
