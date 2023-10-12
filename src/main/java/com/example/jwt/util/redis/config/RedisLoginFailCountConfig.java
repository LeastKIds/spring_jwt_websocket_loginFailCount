package com.example.jwt.util.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisLoginFailCountConfig extends RedisConfig{
    
    @Bean
    public RedisConnectionFactory loginFailCountRedisConnectionFactory() {
        return createLettuceConnectionFactory(1);
    }

    @Bean(name = "loginFailCountTemplate")
    public RedisTemplate<String, Object> loginFailCountTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(loginFailCountRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
