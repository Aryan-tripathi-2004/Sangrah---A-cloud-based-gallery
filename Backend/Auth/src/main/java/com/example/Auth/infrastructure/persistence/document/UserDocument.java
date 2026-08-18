package com.example.Auth.infrastructure.persistence.document;

import com.example.Auth.shared.enums.Role;
import com.example.Auth.shared.enums.UserStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("users")
public class UserDocument {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String displayName;

    private UserStatus status;

    /** Strongly-typed roles — replaces {@code Set<String>}. */
    private Set<Role> roles;

    private Instant createdAt;
    private Instant updatedAt;

    /** Optimistic locking field managed by Spring Data MongoDB. */
    @Version
    private Long version;
}
