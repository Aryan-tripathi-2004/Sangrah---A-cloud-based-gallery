package com.example.Media.api.dto.response;

import com.example.Media.shared.enums.MediaDomain;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable response record representing a media asset returned to API consumers.
 *
 * <p>Design notes:
 * <ul>
 *   <li>A Java {@code record} replaces the mutable Lombok {@code @Data} class. Records are
 *       inherently thread-safe, JSON-serialisable by Jackson without extra configuration,
 *       and communicate immutable intent to all callers.</li>
 *   <li>{@code domain} is typed as {@link MediaDomain} enum. Jackson serialises it as its
 *       {@link Enum#name()} string (e.g., {@code "GALLERY"}), maintaining full API
 *       backward-compatibility with existing consumers.</li>
 *   <li>{@code status} is a derived value ({@code "ACTIVE"} or {@code "DELETED"}) computed
 *       by {@code MediaMapper} at mapping time using a MapStruct expression. It is not a
 *       stored field on the document.</li>
 *   <li>Internal storage fields ({@code contentType}, {@code storageProvider},
 *       {@code storageKey}, {@code updatedAt}, {@code deletedAt}) that are infrastructure
 *       concerns are intentionally omitted — the response exposes only what the API
 *       consumer legitimately needs.</li>
 * </ul>
 * </p>
 */
public record MediaResponse(

        String id,
        String userId,
        MediaDomain domain,
        String entityRefId,
        String fileName,
        String originalFileName,
        String mimeType,
        Long sizeBytes,
        String storageKey,
        String checksumSha256,
        Map<String, Object> metadata,
        Instant uploadedAt,
        Instant createdAt,

        /** Derived lifecycle indicator: {@code "ACTIVE"} while live, {@code "DELETED"} after soft-delete. */
        String status

) {}
