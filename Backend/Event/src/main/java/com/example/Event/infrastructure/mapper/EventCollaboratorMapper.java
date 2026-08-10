package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.CollaboratorMutationResponse;
import com.example.Event.api.dto.response.CollaboratorResponse;
import com.example.Event.api.dto.response.CollaboratorsResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class EventCollaboratorMapper {

    @Autowired
    private UserServiceClient userServiceClient;

    public CollaboratorMutationResponse toMutationResponse(CollaboratorResponse collaborator, String message) {
        return new CollaboratorMutationResponse(collaborator, message);
    }

    public CollaboratorsResponse toCollaboratorsResponse(String eventId, List<CollaboratorResponse> collaborators) {
        return new CollaboratorsResponse(eventId, collaborators);
    }

    public MessageResponse toMessageResponse(String message) {
        return new MessageResponse(message);
    }

    public CollaboratorResponse toEnrichedResponse(EventDocument.EventCollaborator collaborator, String knownEmail) {
        if (collaborator == null) {
            return null;
        }
        String displayName;
        try {
            displayName = userServiceClient.getUserDisplayName(collaborator.getUserId());
        } catch (Exception e) {
            log.warn("Could not fetch displayName for {}: {}", collaborator.getUserId(), e.getMessage());
            displayName = collaborator.getUserId();
        }
        return toResponse(collaborator, knownEmail, displayName);
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
