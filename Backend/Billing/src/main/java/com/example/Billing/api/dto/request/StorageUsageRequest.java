package com.example.Billing.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for recording storage usage ledger entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record storage usage")
public class StorageUsageRequest {

    @NotBlank(message = "User ID is required")
    @Schema(description = "User ID", example = "user123")
    private String userId;

    @NotNull(message = "Storage used (bytes) is required")
    @Schema(description = "Storage used in bytes", example = "1024000")
    private Long bytesUsed;

    @NotBlank(message = "Record type is required")
    @Schema(description = "Type of storage usage (UPLOAD, DELETE, etc)", example = "UPLOAD")
    private String recordType;

    @Schema(description = "Metadata about the storage usage")
    private String metadata;
}
