package com.example.Auth.dao;

import com.example.Auth.entity.Session;
import com.example.Auth.model.SessionModel;
import com.example.Auth.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO for Session operations.
 * Handles data access logic and transformations between Session entity and
 * SessionModel.
 */
@Component
public class SessionDao extends BaseDao<Session, SessionModel, UUID> {

    @Autowired
    private SessionRepository sessionRepository;

    @Override
    protected Class<Session> getEntityClass() {
        return Session.class;
    }

    @Override
    protected SessionModel toModel(Session entity) {
        if (entity == null) {
            return null;
        }

        SessionModel model = new SessionModel();
        model.setSessionId(entity.getSessionId());
        model.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        model.setCreatedAt(entity.getCreatedAt());
        model.setExpiresAt(entity.getExpiresAt());
        model.setLastAccessedAt(entity.getLastAccessedAt());
        model.setIpAddress(entity.getIpAddress());
        model.setUserAgent(entity.getUserAgent());
        model.setDeviceFingerprint(entity.getDeviceFingerprint());
        model.setRevoked(entity.isRevoked());
        model.setRevokedAt(entity.getRevokedAt());
        model.setRevocationReason(entity.getRevocationReason());

        return model;
    }

    @Override
    protected Session toEntity(SessionModel model) {
        if (model == null) {
            return null;
        }

        Session entity = new Session();
        entity.setSessionId(model.getSessionId());
        // User is handled separately (DBRef)
        entity.setCreatedAt(model.getCreatedAt());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setLastAccessedAt(model.getLastAccessedAt());
        entity.setIpAddress(model.getIpAddress());
        entity.setUserAgent(model.getUserAgent());
        entity.setDeviceFingerprint(model.getDeviceFingerprint());
        entity.setRevoked(model.isRevoked());
        entity.setRevokedAt(model.getRevokedAt());
        entity.setRevocationReason(model.getRevocationReason());

        return entity;
    }

    // Business-specific query methods

    /**
     * Find all active sessions for a user.
     *
     * @param userId the user ID
     * @param now    current timestamp
     * @return list of active sessions
     */
    public List<SessionModel> findActiveSessionsByUserId(UUID userId, Instant now) {
        List<Session> entities = sessionRepository.findActiveSessionsByUserId(userId, now);
        return entities.stream().map(this::toModel).toList();
    }

    /**
     * Find recent sessions for a user.
     *
     * @param userId the user ID
     * @param limit  maximum number of sessions to return
     * @return list of recent sessions
     */
    public List<SessionModel> findRecentSessionsByUserId(UUID userId, int limit) {
        List<Session> entities = sessionRepository.findByUserId(userId);
        return entities.stream().limit(limit).map(this::toModel).toList();
    }

    /**
     * Find expired sessions.
     *
     * @param now current timestamp
     * @return list of expired sessions
     */
    public List<SessionModel> findExpiredSessions(Instant now) {
        List<Session> entities = sessionRepository.findExpiredSessions(now);
        return entities.stream().map(this::toModel).toList();
    }

    /**
     * Count active sessions for a user.
     *
     * @param userId the user ID
     * @param now    current timestamp
     * @return count of active sessions
     */
    public long countActiveSessionsByUserId(UUID userId, Instant now) {
        return sessionRepository.countActiveSessionsByUserId(userId, now);
    }

    /**
     * Check if a session exists and is active.
     *
     * @param sessionId the session ID
     * @param now       current timestamp
     * @return true if session is active
     */
    public boolean existsActiveSession(UUID sessionId, Instant now) {
        Query query = new Query(Criteria.where("_id").is(sessionId)
                .and("revoked").is(false)
                .and("expiresAt").gt(now));
        return exists(query);
    }

    // Update operations using MongoTemplate

    /**
     * Update last accessed timestamp for a session.
     *
     * @param sessionId      the session ID
     * @param lastAccessedAt the last accessed timestamp
     * @return true if updated successfully
     */
    public boolean updateLastAccessed(UUID sessionId, Instant lastAccessedAt) {
        Query query = new Query(Criteria.where("_id").is(sessionId));
        Update update = new Update().set("lastAccessedAt", lastAccessedAt);
        return updateOne(query, update);
    }

    /**
     * Extend session expiry.
     *
     * @param sessionId the session ID
     * @param expiresAt the new expiry timestamp
     * @return true if updated successfully
     */
    public boolean extendExpiry(UUID sessionId, Instant expiresAt) {
        Query query = new Query(Criteria.where("_id").is(sessionId));
        Update update = new Update().set("expiresAt", expiresAt);
        return updateOne(query, update);
    }

    /**
     * Revoke a session.
     *
     * @param sessionId the session ID
     * @param reason    the revocation reason
     * @return true if revoked successfully
     */
    public boolean revokeSession(UUID sessionId, String reason) {
        Query query = new Query(Criteria.where("_id").is(sessionId));
        Update update = new Update()
                .set("revoked", true)
                .set("revokedAt", Instant.now())
                .set("revocationReason", reason);
        return updateOne(query, update);
    }

    /**
     * Revoke all sessions for a user.
     *
     * @param userId the user ID
     * @param reason the revocation reason
     * @return number of sessions revoked
     */
    public long revokeAllSessionsForUser(UUID userId, String reason) {
        Query query = new Query(Criteria.where("user.$id").is(userId).and("revoked").is(false));
        Update update = new Update()
                .set("revoked", true)
                .set("revokedAt", Instant.now())
                .set("revocationReason", reason);
        return update(query, update);
    }

    /**
     * Revoke all other sessions except the current one.
     *
     * @param userId           the user ID
     * @param currentSessionId the session ID to keep active
     * @param reason           the revocation reason
     * @return number of sessions revoked
     */
    public long revokeOtherSessions(UUID userId, UUID currentSessionId, String reason) {
        Query query = new Query(Criteria.where("user.$id").is(userId)
                .and("_id").ne(currentSessionId)
                .and("revoked").is(false));
        Update update = new Update()
                .set("revoked", true)
                .set("revokedAt", Instant.now())
                .set("revocationReason", reason);
        return update(query, update);
    }
}
