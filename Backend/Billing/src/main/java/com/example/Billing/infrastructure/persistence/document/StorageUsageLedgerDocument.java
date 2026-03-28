package com.example.Billing.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("storage_usage_ledger")
public class StorageUsageLedgerDocument {
    @Id
    private String id;
    private String userId;
    private String domain;
    private String domainRefId;
    private long sizeBytes;
    private Instant startAt;
    private Instant endAt;
    private String sourceService;
    private Instant createdAt;
}
