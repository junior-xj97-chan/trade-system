package com.trade.quote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tushare")
public class TushareConfig {

    private String token;

    private int pollIntervalMs = 1000;

    private int threadPoolSize = 4;
}
