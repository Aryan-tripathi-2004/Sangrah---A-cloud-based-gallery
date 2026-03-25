package com.example.Auth.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("refresh_tokens")
public class RefreshTokenDocument {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String token;

    @Indexed(expireAfterSeconds = 0)  // TTL Index - auto-delete when expiresAt is reached
    private Instant expiresAt;

    private Instant createdAt;
    private Instant revokedAt;  // null if not revoked

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
