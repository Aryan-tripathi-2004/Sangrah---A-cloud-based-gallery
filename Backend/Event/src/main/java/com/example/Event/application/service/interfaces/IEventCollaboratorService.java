package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.request.AddCollaboratorRequest;
import com.example.Event.api.dto.request.UpdateCollaboratorPermissionsRequest;
import com.example.Event.api.dto.response.CollaboratorMutationResponse;
import com.example.Event.api.dto.response.CollaboratorsResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface IEventCollaboratorService {
    CollaboratorMutationResponse addCollaborator(String eventId, AddCollaboratorRequest request, HttpServletRequest httpRequest);

    CollaboratorsResponse getCollaborators(String eventId);

    MessageResponse removeCollaborator(String eventId, String collaboratorUserId, HttpServletRequest httpRequest);

    CollaboratorMutationResponse updateCollaboratorPermissions(
            String eventId,
            String collaboratorUserId,
            UpdateCollaboratorPermissionsRequest request,
            HttpServletRequest httpRequest);

    boolean hasPermission(String eventId, String userId, String permission);

    boolean isCollaborator(String eventId, String userId);

    List<EventDocument.EventCollaborator> getCollaboratorDocuments(String eventId);
}
