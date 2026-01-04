package com.example.Auth.dao;

import com.example.Auth.entity.Session;
import com.example.Auth.model.SessionModel;
import com.example.Auth.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            return !session.isRevoked() && session.getExpiresAt().isAfter(now);
        }
        return false;
    }

    // Update operations using JPA

    /**
     * Update last accessed timestamp for a session.
     *
     * @param sessionId      the session ID
     * @param lastAccessedAt the last accessed timestamp
     * @return true if updated successfully
     */
    @Transactional
    public boolean updateLastAccessed(UUID sessionId, Instant lastAccessedAt) {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            session.setLastAccessedAt(lastAccessedAt);
            sessionRepository.save(session);
            return true;
        }
        return false;
    }

    /**
     * Extend session expiry.
     *
     * @param sessionId the session ID
     * @param expiresAt the new expiry timestamp
     * @return true if updated successfully
     */
    @Transactional
    public boolean extendExpiry(UUID sessionId, Instant expiresAt) {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            session.setExpiresAt(expiresAt);
            sessionRepository.save(session);
            return true;
        }
        return false;
    }

    /**
     * Revoke a session.
     *
     * @param sessionId the session ID
     * @param reason    the revocation reason
     * @return true if revoked successfully
     */
    @Transactional
    public boolean revokeSession(UUID sessionId, String reason) {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            session.setRevoked(true);
            session.setRevokedAt(Instant.now());
            session.setRevocationReason(reason);
            sessionRepository.save(session);
            return true;
        }
        return false;
    }

    /**
     * Revoke all sessions for a user.
     *
     * @param userId the user ID
     * @param reason the revocation reason
     * @return number of sessions revoked
     */
    @Transactional
    public long revokeAllSessionsForUser(UUID userId, String reason) {
        List<Session> sessions = sessionRepository.findByUserId(userId);
        long count = 0;
        Instant now = Instant.now();
        for (Session session : sessions) {
            if (!session.isRevoked()) {
                session.setRevoked(true);
                session.setRevokedAt(now);
                session.setRevocationReason(reason);
                sessionRepository.save(session);
                count++;
            }
        }
        return count;
    }

    /**
     * Revoke all other sessions except the current one.
     *
     * @param userId           the user ID
     * @param currentSessionId the session ID to keep active
     * @param reason           the revocation reason
     * @return number of sessions revoked
     */
    @Transactional
    public long revokeOtherSessions(UUID userId, UUID currentSessionId, String reason) {
        List<Session> sessions = sessionRepository.findByUserId(userId);
        long count = 0;
        Instant now = Instant.now();
        for (Session session : sessions) {
            if (!session.getSessionId().equals(currentSessionId) && !session.isRevoked()) {
                session.setRevoked(true);
                session.setRevokedAt(now);
                session.setRevocationReason(reason);
                sessionRepository.save(session);
                count++;
            }
        }
        return count;
    }
}
