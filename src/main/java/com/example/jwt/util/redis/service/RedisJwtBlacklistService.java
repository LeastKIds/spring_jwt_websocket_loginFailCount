package com.example.jwt.util.redis.service;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
// @RequiredArgsConstructor
public class RedisJwtBlacklistService {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisJwtBlacklistService(@Qualifier("blackListRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setBlackList(String token, String userEmail, long seconds) {
        // HashMap을 생성하여 토큰과 유저의 이메일을 저장
        HashMap<String, String> map = new HashMap<>();
        map.put("token", token);
        map.put("userEmail", userEmail);

        // Redis에 토큰 값을 키로 하여 HashMap 값을 저장
        redisTemplate.opsForHash().putAll(token, map);

        // 해당 토큰 값을 키로 가진 Hash에 만료 시간 설정
        redisTemplate.expire(token, seconds, TimeUnit.MILLISECONDS);
    }
    public boolean hasKeyBlackList(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
