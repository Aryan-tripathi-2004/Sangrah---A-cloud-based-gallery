package com.example.Auth.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
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
    private String status;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
