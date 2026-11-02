package com.example.Billing.infrastructure.mapper;

import com.example.Billing.api.dto.request.StorageUsageRequest;
import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.api.dto.response.StorageUsageResponse;
import com.example.Billing.infrastructure.persistence.document.StorageUsageLedgerDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StorageUsageMapper {

    @Mapping(target = "bytesUsed", source = "sizeBytes")
    @Mapping(target = "recordDate", source = "startAt")
    StorageUsageResponse toResponse(StorageUsageLedgerDocument document);

    StorageUsageLedgerDTO toLedgerDTO(StorageUsageLedgerDocument document);

    @Mapping(target = "sizeBytes", source = "bytesUsed")
    StorageUsageLedgerDocument toDocument(StorageUsageRequest request);

    default LocalDateTime map(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
