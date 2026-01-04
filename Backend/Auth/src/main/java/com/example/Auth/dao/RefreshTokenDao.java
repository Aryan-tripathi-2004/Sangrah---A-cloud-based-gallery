package com.example.Auth.dao;

import com.example.Auth.entity.RefreshToken;
import com.example.Auth.model.RefreshTokenModel;
import com.example.Auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        model.setLastUsedAt(entity.getLastUsedAt());

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
        entity.setLastUsedAt(model.getLastUsedAt());

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

    @Transactional
    public boolean revokeToken(String tokenHash) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            return true;
        }
        return false;
    }

    @Transactional
    public long revokeAllTokensForUser(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        long count = 0;
        Instant now = Instant.now();
        for (RefreshToken token : tokens) {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public long revokeAllTokensForSession(UUID sessionId) {
        List<RefreshToken> tokens = refreshTokenRepository.findBySessionId(sessionId);
        long count = 0;
        Instant now = Instant.now();
        for (RefreshToken token : tokens) {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public boolean updateLastUsed(UUID tokenId) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findById(tokenId);
        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            token.setLastUsedAt(Instant.now());
            refreshTokenRepository.save(token);
            return true;
        }
        return false;
    }
}
