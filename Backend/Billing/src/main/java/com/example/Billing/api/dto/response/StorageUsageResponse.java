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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage usage ledger entry")
public class StorageUsageResponse {

    @Schema(description = "Ledger entry ID", example = "507f1f77bcf86cd799439011")
    private String id;

    @Schema(description = "User ID", example = "user123")
    private String userId;

    @Schema(description = "Storage used in bytes", example = "1024000")
    private Long bytesUsed;

    @Schema(description = "Cost in currency units", example = "0.50")
    private BigDecimal cost;

    @Schema(description = "Type of storage usage", example = "UPLOAD")
    private String recordType;

    @Schema(description = "Record date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recordDate;

    @Schema(description = "Entry creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
