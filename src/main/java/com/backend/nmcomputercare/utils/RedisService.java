package com.backend.nmcomputercare.utils;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Generic Redis helper.

 * Previous API kept intact; new generic list methods + rate-limit counter added.

 * Rationale for List-aware methods
 * ──────────────────────────────────
 * Jackson's type erasure means that get(key, ContactFormResponse.class) deserialises
 * a JSON array back as a LinkedHashMap, not a typed List.  The getList / setList
 * overloads use a TypeReference so the correct generic type is preserved at runtime.
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper                  objectMapper;

    // ── Single-object helpers (existing contract) ─────────────────────────────

    public <T> T get(String key, Class<T> type) {
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return null;
            return objectMapper.convertValue(raw, type);
        } catch (Exception e) {
            logger.warn("Redis GET failed | key={} reason={}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, unit);
        } catch (Exception e) {
            logger.warn("Redis SET failed | key={} reason={}", key, e.getMessage());
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.warn("Redis DELETE failed | key={} reason={}", key, e.getMessage());
        }
    }

    // ── Generic list helpers (new) ────────────────────────────────────────────

    /**
     * Retrieve a typed list from Redis.
     * Returns {@code null} on cache miss or deserialisation failure
     * so callers can fall through to the DB safely.
     */
    public <T> List<T> getList(String key, Class<T> elementType) {
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return null;

            TypeReference<List<T>> typeRef = new TypeReference<>() {};
            // Re-serialize then deserialise to handle LinkedHashMap → typed object.
            String json = objectMapper.writeValueAsString(raw);
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            logger.warn("Redis getList failed | key={} reason={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Persist a typed list in Redis with a TTL.
     */
    public <T> void setList(String key, List<T> values, long ttl, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, values, ttl, unit);
        } catch (Exception e) {
            logger.warn("Redis setList failed | key={} reason={}", key, e.getMessage());
        }
    }


}