package com.example.jwt.util.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisJwtBlacklistConfig extends RedisConfig{
    
    @Bean
    @Primary
    public RedisConnectionFactory blacklistRedisConnectionFactory() {
        return createLettuceConnectionFactory(0);
    }

    @Bean(name = "blackListRedisTemplate")
    public RedisTemplate<String, Object> blacklistRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(blacklistRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
