package com.example.Auth.infrastructure.persistence.repository;

import com.example.Auth.infrastructure.persistence.document.RefreshTokenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshTokenDocument, String> {
    Optional<RefreshTokenDocument> findByToken(String token);

    void deleteByUserId(String userId);
    long countByUserIdAndRevokedAtIsNull(String userId);

}
