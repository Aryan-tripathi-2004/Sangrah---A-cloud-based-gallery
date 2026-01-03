package com.example.Auth.dao;

import com.example.Auth.entity.RefreshToken;
import com.example.Auth.model.RefreshTokenModel;
import com.example.Auth.repository.RefreshTokenRepository;
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
 * DAO for RefreshToken operations.
 */
@Component
public class RefreshTokenDao extends BaseDao<RefreshToken, RefreshTokenModel, UUID> {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    protected Class<RefreshToken> getEntityClass() {
        return RefreshToken.class;
    }

    @Override
    protected RefreshTokenModel toModel(RefreshToken entity) {
        if (entity == null)
            return null;

        RefreshTokenModel model = new RefreshTokenModel();
        model.setId(entity.getId());
        model.setTokenHash(entity.getTokenHash());
        model.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        model.setSessionId(entity.getSessionId());
        model.setExpiresAt(entity.getExpiresAt());
        model.setRevoked(entity.isRevoked());
        model.setRevokedAt(entity.getRevokedAt());
        model.setCreatedAt(entity.getIssuedAt());

        return model;
    }

    @Override
    protected RefreshToken toEntity(RefreshTokenModel model) {
        if (model == null)
            return null;

        RefreshToken entity = new RefreshToken();
        entity.setId(model.getId());
        entity.setTokenHash(model.getTokenHash());
        entity.setSessionId(model.getSessionId());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setRevoked(model.isRevoked());
        entity.setRevokedAt(model.getRevokedAt());
        entity.setIssuedAt(model.getCreatedAt());

        return entity;
    }

    // Query methods

    public Optional<RefreshTokenModel> findByTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash).map(this::toModel);
    }

    public List<RefreshTokenModel> findValidTokensByUserId(UUID userId, Instant now) {
        return refreshTokenRepository.findValidTokensByUserId(userId, now).stream()
                .map(this::toModel).toList();
    }

    public List<RefreshTokenModel> findBySessionId(UUID sessionId) {
        return refreshTokenRepository.findBySessionId(sessionId).stream()
                .map(this::toModel).toList();
    }

    public long countActiveTokensByUserId(UUID userId, Instant now) {
        return refreshTokenRepository.countActiveTokensByUserId(userId, now);
    }

    // Update operations

    public boolean revokeToken(String tokenHash) {
        Query query = new Query(Criteria.where("tokenHash").is(tokenHash));
        Update update = new Update()
                .set("revoked", true)
                .set("revokedAt", Instant.now());
        return updateOne(query, update);
    }

    public long revokeAllTokensForUser(UUID userId) {
        Query query = new Query(Criteria.where("user.$id").is(userId).and("revoked").is(false));
        Update update = new Update()
                .set("revoked", true)
                .set("revokedAt", Instant.now());
        return update(query, update);
    }

    public long revokeAllTokensForSession(UUID sessionId) {
        Query query = new Query(Criteria.where("sessionId").is(sessionId).and("revoked").is(false));
        Update update = new Update()
                .set("revoked", true)
                .set("revokedAt", Instant.now());
        return update(query, update);
    }

    public boolean updateLastUsed(UUID tokenId) {
        Query query = new Query(Criteria.where("_id").is(tokenId));
        Update update = new Update().set("lastUsedAt", Instant.now());
        return updateOne(query, update);
    }
}
