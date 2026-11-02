package com.example.Email.infrastructure.mapper;

import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting Email persistence documents to API DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmailMapper {

    @Mapping(target = "status", source = "status")
    @Mapping(target = "emailId", source = "emailId")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "message", expression = "java(\"Email processed with status: \" + document.getStatus())")
    @Mapping(target = "errorReason", source = "failureReason")
    EmailSendResponse toResponse(EmailLogDocument document);
}
