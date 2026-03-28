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
    private String reason;  // "LOGOUT", "TOKEN_REFRESH", etc.

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
