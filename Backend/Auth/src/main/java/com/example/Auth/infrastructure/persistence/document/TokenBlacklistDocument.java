package com.example.Auth.infrastructure.persistence.document;

import com.example.Auth.shared.enums.RevocationReason;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("token_blacklist")
public class TokenBlacklistDocument {
    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userId;

    @Indexed(expireAfterSeconds = 0)  // TTL Index - auto-delete when expiresAt is reached
    private Instant expiresAt;

    private Instant blacklistedAt;

    /** Strongly-typed revocation reason — replaces raw {@code String reason}. */
    private RevocationReason reason;

    /** Optimistic locking field managed by Spring Data MongoDB. */
    @Version
    private Long version;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
