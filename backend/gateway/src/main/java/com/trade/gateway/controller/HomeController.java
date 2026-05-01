package com.trade.gateway.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 首页 Controller
 * 处理 Gateway 根路径请求
 */
@RestController
public class HomeController {

    private static final String HTML_HOME = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>金融交易系统 - API Gateway</title>
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
                        max-width: 600px;
                        margin: 20px;
                    }
                    h1 { font-size: 32px; color: #333; margin-bottom: 10px; }
                    .subtitle { font-size: 18px; color: #666; margin-bottom: 40px; }
                    .services {
                        display: grid;
                        grid-template-columns: repeat(2, 1fr);
                        gap: 15px;
                        text-align: left;
                    }
                    .service {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 10px;
                    }
                    .service-name { font-weight: bold; color: #667eea; margin-bottom: 5px; }
                    .service-desc { font-size: 13px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div style="font-size: 60px; margin-bottom: 20px;">🚀</div>
                    <h1>金融交易系统</h1>
                    <p class="subtitle">Spring Cloud Alibaba 微服务架构</p>
                    <div class="services">
                        <div class="service">
                            <div class="service-name">用户服务</div>
                            <div class="service-desc">/api/user/**</div>
                        </div>
                        <div class="service">
                            <div class="service-name">订单服务</div>
                            <div class="service-desc">/api/order/**</div>
                        </div>
                        <div class="service">
                            <div class="service-name">交易服务</div>
                            <div class="service-desc">/api/trade/**</div>
                        </div>
                        <div class="service">
                            <div class="service-name">账户服务</div>
                            <div class="service-desc">/api/account/**</div>
                        </div>
                        <div class="service">
                            <div class="service-name">商品服务</div>
                            <div class="service-desc">/api/product/**</div>
                        </div>
                        <div class="service">
                            <div class="service-name">搜索服务</div>
                            <div class="service-desc">/api/search/**</div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<String> home() {
        return Mono.just(HTML_HOME);
    }
}
