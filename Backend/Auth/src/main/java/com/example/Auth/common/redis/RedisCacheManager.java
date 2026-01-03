package com.example.Auth.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Helper class for Redis cache operations.
 */
public class RedisCacheManager {

    private static final DashLogger logger = DashLoggerFactory.getLogger(RedisCacheManager.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheManager(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Set a cache entry with TTL.
     *
     * @param key        the cache key
     * @param value      the value to cache
     * @param ttlSeconds the TTL in seconds
     * @param <T>        the type of the value
     */
    public <T> void set(String key, T value, long ttlSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
            logger.debug("Cache set: " + key);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize value for cache key: " + key, e);
        }
    }

    /**
     * Set a cache entry without TTL (permanent).
     *
     * @param key   the cache key
     * @param value the value to cache
     * @param <T>   the type of the value
     */
    public <T> void set(String key, T value) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue);
            logger.debug("Cache set (permanent): " + key);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize value for cache key: " + key, e);
        }
    }

    /**
     * Get a cache entry.
     *
     * @param key   the cache key
     * @param clazz the class type to deserialize to
     * @param <T>   the type of the value
     * @return the cached value, or null if not found or deserialization fails
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(key);
            if (jsonValue == null) {
                logger.debug("Cache miss: " + key);
                return null;
            }
            logger.debug("Cache hit: " + key);
            return objectMapper.readValue(jsonValue, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize value for cache key: " + key, e);
            return null;
        }
    }

    /**
     * Delete a cache entry.
     *
     * @param key the cache key
     * @return true if the key was deleted, false otherwise
     */
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(result)) {
            logger.debug("Cache deleted: " + key);
            return true;
        }
        return false;
    }

    /**
     * Check if a key exists in cache.
     *
     * @param key the cache key
     * @return true if the key exists, false otherwise
     */
    public boolean exists(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Set the expiration time for a key.
     *
     * @param key        the cache key
     * @param ttlSeconds the TTL in seconds
     * @return true if the expiration was set, false otherwise
     */
    public boolean expire(String key, long ttlSeconds) {
        Boolean result = redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Get the remaining TTL for a key.
     *
     * @param key the cache key
     * @return the TTL in seconds, or -1 if the key doesn't exist, -2 if no TTL is
     *         set
     */
    public long getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }

    /**
     * Increment a counter.
     *
     * @param key the counter key
     * @return the new value after increment
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * Increment a counter with a delta.
     *
     * @param key   the counter key
     * @param delta the amount to increment by
     * @return the new value after increment
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * Decrement a counter.
     *
     * @param key the counter key
     * @return the new value after decrement
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }
}
