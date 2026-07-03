package com.trade.seckill.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        log.info("【Sentinel】秒杀服务限流规则初始化完成");
    }
}
