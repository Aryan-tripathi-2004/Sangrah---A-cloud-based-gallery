package com.example.Billing.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for storage usage ledger entry response.
 */
@Builder
@Schema(description = "Storage usage ledger entry")
public record StorageUsageResponse(
    @Schema(description = "Ledger entry ID", example = "507f1f77bcf86cd799439011")
    String id,

    @Schema(description = "User ID", example = "user123")
    String userId,

    @Schema(description = "Storage used in bytes", example = "1024000")
    Long bytesUsed,

    @Schema(description = "Cost in currency units", example = "0.50")
    BigDecimal cost,

    @Schema(description = "Type of storage usage", example = "UPLOAD")
    String recordType,

    @Schema(description = "Record date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime recordDate,

    @Schema(description = "Entry creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt
) {}
