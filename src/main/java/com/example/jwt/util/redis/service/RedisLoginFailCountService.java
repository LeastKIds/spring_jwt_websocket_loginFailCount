package com.example.jwt.util.redis.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.jwt.env.auth.AuthEnv;

@Component
// @RequiredArgsConstructor
public class RedisLoginFailCountService {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisLoginFailCountService(@Qualifier("loginFailCountTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int getLoginFailCount(String userEmail) {
        if(Boolean.TRUE.equals(redisTemplate.hasKey(userEmail))) {
            Map<Object, Object> loginFailCount = redisTemplate.opsForHash().entries(userEmail);
            return Integer.valueOf((String) loginFailCount.get("count"));
        } else {
            return -1;
        }
    }

    public void setLoginFailCount(String userEmail, int count) {
        

        if(Boolean.TRUE.equals(redisTemplate.hasKey(userEmail))) {
            redisTemplate.opsForHash().put(userEmail, "count", String.valueOf(count));
        } else {
            AuthEnv authEnv = new AuthEnv();
            HashMap<String, String> loginFailCount = new HashMap<>();
            loginFailCount.put("userEmail", userEmail);
            loginFailCount.put("count", "1");

            redisTemplate.opsForHash().putAll(userEmail, loginFailCount);
            redisTemplate.expire(userEmail, authEnv.getEXPIRE_LOGIN_FAIL(), TimeUnit.MILLISECONDS);
        }
    }

    public void deleteLoginFailCount(String userEmail) {
        if(Boolean.TRUE.equals(redisTemplate.hasKey(userEmail))) {
            redisTemplate.delete(userEmail);
        }
    }
}
