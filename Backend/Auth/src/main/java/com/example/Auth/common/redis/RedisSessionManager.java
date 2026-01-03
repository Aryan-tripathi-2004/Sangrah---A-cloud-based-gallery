package com.example.Auth.common.redis;

import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Helper class for managing user sessions in Redis.
 */
public class RedisSessionManager {

    private static final DashLogger logger = DashLoggerFactory.getLogger(RedisSessionManager.class);
    private static final String SESSION_PREFIX = "sessions:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisSessionManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create a session with session data.
     *
     * @param sessionId   the session ID
     * @param sessionData the session data (usually user ID)
     * @param ttlSeconds  the TTL in seconds
     */
    public void createSession(String sessionId, String sessionData, long ttlSeconds) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, sessionData, ttlSeconds, TimeUnit.SECONDS);
        logger.debug("Session created: " + sessionId);
    }

    /**
     * Get session data.
     *
     * @param sessionId the session ID
     * @return the session data, or null if not found
     */
    public String getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        String data = redisTemplate.opsForValue().get(key);
        if (data != null) {
            logger.debug("Session found: " + sessionId);
        } else {
            logger.debug("Session not found: " + sessionId);
        }
        return data;
    }

    /**
     * Check if a session exists.
     *
     * @param sessionId the session ID
     * @return true if the session exists, false otherwise
     */
    public boolean sessionExists(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Revoke (delete) a session.
     *
     * @param sessionId the session ID
     * @return true if the session was deleted, false otherwise
     */
    public boolean revokeSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Boolean result = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(result)) {
            logger.debug("Session revoked: " + sessionId);
            return true;
        }
        return false;
    }

    /**
     * Extend the TTL of a session.
     *
     * @param sessionId  the session ID
     * @param ttlSeconds the new TTL in seconds
     * @return true if the TTL was updated, false otherwise
     */
    public boolean extendSession(String sessionId, long ttlSeconds) {
        String key = SESSION_PREFIX + sessionId;
        Boolean result = redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(result)) {
            logger.debug("Session TTL extended: " + sessionId);
            return true;
        }
        return false;
    }

    /**
     * Get the remaining TTL of a session.
     *
     * @param sessionId the session ID
     * @return the TTL in seconds, or -1 if not found
     */
    public long getSessionTtl(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}
