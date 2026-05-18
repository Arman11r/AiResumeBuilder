package com.resumeai.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed JWT blacklist.
 * Revoked tokens are stored with a TTL equal to the JWT expiry window so
 * that entries clean up automatically – no manual purge job required.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiry-ms:86400000}")
    private long jwtExpiryMs;

    /**
     * Revoke a token by adding it to the Redis blacklist.
     * The entry lives for exactly jwtExpiryMs milliseconds, matching the
     * token's own lifetime – once it would have expired anyway, the key
     * is also removed from Redis.
     */
    public void blacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        Duration ttl = Duration.ofMillis(jwtExpiryMs);
        redisTemplate.opsForValue().set(key, "revoked", ttl);
        log.debug("Token blacklisted in Redis with TTL {}ms", jwtExpiryMs);
    }

    /**
     * Returns true if the token has been explicitly revoked (i.e. the user
     * called /logout or an admin forcibly invalidated it).
     */
    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }
}
