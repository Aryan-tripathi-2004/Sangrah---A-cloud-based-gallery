package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.CollaboratorMutationResponse;
import com.example.Event.api.dto.response.CollaboratorResponse;
import com.example.Event.api.dto.response.CollaboratorsResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface EventCollaboratorMapper {

    CollaboratorMutationResponse toMutationResponse(CollaboratorResponse collaborator, String message);

    CollaboratorsResponse toCollaboratorsResponse(String eventId, List<CollaboratorResponse> collaborators);

    MessageResponse toMessageResponse(String message);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "displayName", source = "displayName")
    CollaboratorResponse toResponse(
            EventDocument.EventCollaborator collaborator,
            String email,
            String displayName);

    default CollaboratorResponse toResponse(EventDocument.EventCollaborator collaborator, String email) {
        return toResponse(collaborator, email, collaborator != null ? collaborator.getUserId() : null);
    }
}
