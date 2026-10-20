package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.request.AddCollaboratorRequest;
import com.example.Event.api.dto.request.UpdateCollaboratorPermissionsRequest;
import com.example.Event.api.dto.response.CollaboratorMutationResponse;
import com.example.Event.api.dto.response.CollaboratorsResponse;
import com.example.Event.api.dto.response.MessageResponse;

public interface IEventCollaboratorService {
    CollaboratorMutationResponse addCollaborator(String eventId, AddCollaboratorRequest request, String userId);

    CollaboratorsResponse getCollaborators(String eventId);

    MessageResponse removeCollaborator(String eventId, String collaboratorUserId, String userId);

    CollaboratorMutationResponse updateCollaboratorPermissions(
            String eventId,
            String collaboratorUserId,
            UpdateCollaboratorPermissionsRequest request,
            String userId);

    boolean hasPermission(String eventId, String userId, String permission);

    boolean isCollaborator(String eventId, String userId);
}
