package com.example.Media.infrastructure.persistence.repository;

import com.example.Media.infrastructure.persistence.document.MediaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends MongoRepository<MediaDocument, String> {

    // Find by checksum (for deduplication)
    Optional<MediaDocument> findByChecksumSha256AndDeletedAtIsNull(String checksumSha256);

    // Find all media for a user in a domain
    List<MediaDocument> findByUserIdAndDomainAndDeletedAtIsNull(String userId, String domain);

    // Find all media for a user across all domains
    List<MediaDocument> findByUserIdAndDeletedAtIsNull(String userId);

    // Find media by ID (check if exists and not deleted)
    Optional<MediaDocument> findByIdAndDeletedAtIsNull(String id);

    // Find media by domain and entity reference
    List<MediaDocument> findByDomainAndEntityRefIdAndDeletedAtIsNull(String domain, String entityRefId);

    // Find all media (including deleted) for recovery purposes
    List<MediaDocument> findByUserId(String userId);

    // Find media by storage key
    Optional<MediaDocument> findByStorageKey(String storageKey);

    // Count active media for a user
    long countByUserIdAndDeletedAtIsNull(String userId);

    // Count active media in a domain for a user
    long countByUserIdAndDomainAndDeletedAtIsNull(String userId, String domain);
}
