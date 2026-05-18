package com.resumeai.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Uses Redis to store invalidated JWTs. 
// Tokens naturally expire in Redis right when the JWT itself expires, saving us from writing cleanup jobs.
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiry-ms:86400000}")
    private long jwtExpiryMs;

    // Adds the token to Redis. It'll automatically vanish when the JWT's natural lifespan ends.
    public void blacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        Duration ttl = Duration.ofMillis(jwtExpiryMs);
        redisTemplate.opsForValue().set(key, "revoked", ttl);
        log.debug("Token blacklisted in Redis with TTL {}ms", jwtExpiryMs);
    }

    // Checks if the user was forcibly logged out or decided to log out themselves.
    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }
}
