package com.example.Media.infrastructure.mapper;

import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.infrastructure.persistence.document.MediaDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link MediaDocument} and API DTOs.
 *
 * <p>Design notes:
 * <ul>
 *   <li>All manual {@code default} method implementations have been deleted. MapStruct's
 *       annotation processor now generates the full implementation at compile time,
 *       eliminating handwritten boilerplate and the risk of mapping drift.</li>
 *   <li>The redundant {@code @Component} annotation has been removed. {@code @Mapper(componentModel = "spring")}
 *       already instructs MapStruct to generate a {@code @Component}-annotated implementation
 *       class. Stacking {@code @Component} on the interface registers a second, non-functional
 *       bean — a subtle Spring bug that caused ambiguous autowiring.</li>
 *   <li>The {@code status} field is a derived value not present on the source document. It is
 *       mapped via a MapStruct {@code expression} that delegates to the existing
 *       {@link MediaDocument#isActive()} helper method, preserving zero functional regression
 *       from the original manual mapping logic.</li>
 *   <li>The former {@code toDocument(MediaResponse)} method has been intentionally removed.
 *       Converting a response DTO back into a domain entity violates DDD principles — a response
 *       object is a projection for the API layer and should never flow back into the
 *       persistence layer.</li>
 *   <li>Source fields absent from {@link MediaResponse} ({@code contentType}, {@code storageProvider},
 *       {@code updatedAt}, {@code deletedAt}, {@code version}) are silently ignored by
 *       MapStruct's default {@code unmappedSourcePolicy = IGNORE}.</li>
 * </ul>
 * </p>
 */
@Mapper(componentModel = "spring")
public interface MediaMapper {

    /**
     * Maps a {@link MediaDocument} to a {@link MediaResponse}.
     *
     * <p>All same-name, same-type fields are auto-mapped by MapStruct.
     * The {@code status} field is the only field requiring an explicit expression
     * because it is a derived value with no corresponding field on the source.</p>
     */
    @Mapping(
        target = "status",
        expression = "java(document.isActive() ? \"ACTIVE\" : \"DELETED\")"
    )
    MediaResponse toResponse(MediaDocument document);
}
