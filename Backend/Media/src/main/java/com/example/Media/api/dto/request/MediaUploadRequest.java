package com.example.Media.api.dto.request;

import com.example.Media.shared.enums.MediaDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Immutable request record carrying the metadata for a media upload operation.
 *
 * <p>Design notes:
 * <ul>
 *   <li>A Java {@code record} is used instead of a Lombok {@code @Data} class to enforce
 *       compile-time immutability. Records have no setters, preventing accidental mutation
 *       after binding.</li>
 *   <li>{@code domain} is typed as {@link MediaDomain} enum. Spring's {@code ConversionService}
 *       automatically converts the incoming {@code @RequestParam} string (e.g., "GALLERY")
 *       to the enum constant via {@code Enum.valueOf()}, making invalid domain values a
 *       self-documenting {@code 400 Bad Request} with zero custom code.</li>
 *   <li>The {@link org.springframework.web.multipart.MultipartFile} is intentionally excluded
 *       from this record. The file binary is a separate concern bound via
 *       {@code @RequestParam("file")} in the controller to keep multipart handling clean.</li>
 * </ul>
 * </p>
 */
public record MediaUploadRequest(

        @NotBlank(message = "fileName must not be blank")
        @Size(max = 255, message = "fileName must not exceed 255 characters")
        String fileName,

        @NotNull(message = "domain must not be null")
        MediaDomain domain,

        // Optional: populated for event media, profile avatars, etc.
        String entityRefId,

        @NotNull(message = "sizeBytes must not be null")
        @Positive(message = "sizeBytes must be a positive value")
        Long sizeBytes,

        // Optional: pre-computed by the client for integrity verification
        String checksumSha256,

        // Optional: arbitrary key-value pairs for domain-specific context
        Map<String, Object> metadata

) {}
