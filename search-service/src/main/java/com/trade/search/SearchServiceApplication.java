package com.trade.search;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 搜索服务启动类
 * 基于 Elasticsearch 提供商品搜索功能
 */
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.trade.search.mapper")
@EnableElasticsearchRepositories(basePackages = "com.trade.search.repository")
@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.trade.search", "com.trade.common"})
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
