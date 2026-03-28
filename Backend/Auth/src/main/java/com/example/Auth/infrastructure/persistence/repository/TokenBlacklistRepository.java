package com.example.Auth.infrastructure.persistence.repository;

import com.example.Auth.infrastructure.persistence.document.TokenBlacklistDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends MongoRepository<TokenBlacklistDocument, String> {
    Optional<TokenBlacklistDocument> findByToken(String token);

    boolean existsByToken(String token);
}
