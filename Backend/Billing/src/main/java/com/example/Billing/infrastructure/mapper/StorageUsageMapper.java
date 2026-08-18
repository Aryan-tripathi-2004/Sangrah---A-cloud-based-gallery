package com.example.Billing.infrastructure.mapper;

import com.example.Billing.api.dto.request.StorageUsageRequest;
import com.example.Billing.api.dto.response.StorageUsageResponse;
import com.example.Billing.infrastructure.persistence.document.StorageUsageLedgerDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * MapStruct mapper for StorageUsageLedger Entity ↔ DTO conversions.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface StorageUsageMapper {

    /**
     * Convert StorageUsageLedgerDocument to StorageUsageResponse DTO
     */
    StorageUsageResponse toResponse(StorageUsageLedgerDocument document);

    /**
     * Convert StorageUsageRequest DTO to StorageUsageLedgerDocument
     */
    StorageUsageLedgerDocument toDocument(StorageUsageRequest request);

    default LocalDateTime map(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.of("UTC"));
    }
}
