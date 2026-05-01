package com.trade.gateway.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Gateway 响应式 Redis 配置
 * <p>
 * Gateway 基于 WebFlux，必须使用 ReactiveRedisTemplate 而非阻塞式 RedisTemplate。
 * 此配置类提供 ReactiveRedisTemplate<String, Object> Bean，
 * 与其他微服务的 RedisTemplate<String, Object> 保持相同的序列化策略，
 * 确保数据读写兼容。
 */
@Configuration
public class ReactiveRedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        // Key 使用 String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Value 使用 Jackson JSON 序列化（与 common.RedisConfig 保持一致）
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        // 构建序列化上下文
        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext(stringSerializer)
                .key(stringSerializer)
                .hashKey(stringSerializer)
                .value(jsonSerializer)
                .hashValue(jsonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
