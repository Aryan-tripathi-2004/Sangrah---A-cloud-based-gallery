package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.CollaboratorMutationResponse;
import com.example.Event.api.dto.response.CollaboratorResponse;
import com.example.Event.api.dto.response.CollaboratorsResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class EventCollaboratorMapper {

    public CollaboratorMutationResponse toMutationResponse(CollaboratorResponse collaborator, String message) {
        return new CollaboratorMutationResponse(collaborator, message);
    }

    public CollaboratorsResponse toCollaboratorsResponse(String eventId, List<CollaboratorResponse> collaborators) {
        return new CollaboratorsResponse(eventId, collaborators);
    }

    public MessageResponse toMessageResponse(String message) {
        return new MessageResponse(message);
    }

    public CollaboratorResponse toResponse(EventDocument.EventCollaborator collaborator, String email) {
        return toResponse(collaborator, email, collaborator != null ? collaborator.getUserId() : null);
    }

    public CollaboratorResponse toResponse(
            EventDocument.EventCollaborator collaborator,
            String email,
            String displayName) {
        if (collaborator == null) {
            return null;
        }
        return new CollaboratorResponse(
                collaborator.getUserId(),
                collaborator.getCanUploadMedia(),
                collaborator.getCanReviewMedia(),
                collaborator.getCanReviewAccessRequests(),
                collaborator.getCanDirectUpload(),
                collaborator.getCanDeleteMedia(),
                collaborator.getCanEditEventDetails(),
                collaborator.getAddedAt(),
                collaborator.getAddedByUserId(),
                email,
                displayName);
    }
}
