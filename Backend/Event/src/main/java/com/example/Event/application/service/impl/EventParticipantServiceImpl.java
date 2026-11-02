package com.example.Event.application.service.impl;

import com.example.Event.api.dto.response.EventParticipantsResponse;
import com.example.Event.application.service.interfaces.IEventParticipantService;
import com.example.Event.infrastructure.mapper.EventParticipantMapper;
import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.infrastructure.persistence.repository.EventRepository;
import com.example.Event.shared.enums.ApprovalStatus;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventParticipantServiceImpl implements IEventParticipantService {

    private final EventRepository eventRepository;
    private final EventAccessRequestRepository accessRequestRepository;
    private final EventParticipantMapper participantMapper;

    @Override
    public EventParticipantsResponse listParticipants(String eventId, String requesterUserId) {
        log.info("👥 Listing participants for event: {} by user: {}", eventId, requesterUserId);

        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        boolean isOwnerOrCollab = (requesterUserId != null && requesterUserId.equals(event.getOwnerUserId())) ||
                (event.getCollaborators() != null && event.getCollaborators().stream()
                        .anyMatch(c -> requesterUserId != null && requesterUserId.equals(c.getUserId())));

        if (!isOwnerOrCollab) {
            throw new ForbiddenOperationException("Only the event owner or collaborators can view participants");
        }

        Set<String> participantUserIds = new LinkedHashSet<>();
        if (event.getOwnerUserId() != null && !event.getOwnerUserId().isBlank()) {
            participantUserIds.add(event.getOwnerUserId());
        }
        if (event.getCollaborators() != null) {
            for (EventDocument.EventCollaborator collaborator : event.getCollaborators()) {
                if (collaborator.getUserId() != null && !collaborator.getUserId().isBlank()) {
                    participantUserIds.add(collaborator.getUserId());
                }
            }
        }
        List<EventAccessRequestDocument> approvedRequests =
                accessRequestRepository.findByEventIdAndStatus(eventId, ApprovalStatus.APPROVED);
        for (EventAccessRequestDocument request : approvedRequests) {
            if (request.getRequesterUserId() != null && !request.getRequesterUserId().isBlank()) {
                participantUserIds.add(request.getRequesterUserId());
            }
        }

        log.info("👥 Found {} active participants for event {}", participantUserIds.size(), eventId);
        return participantMapper.toParticipantsResponse(eventId, List.copyOf(participantUserIds));
    }

    @Override
    @Transactional
    public void removeParticipant(String eventId, String participantUserId, String requesterUserId) {
        log.info("🚫 Removing participant {} from event {} by requester: {}", participantUserId, eventId, requesterUserId);

        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (!requesterUserId.equals(event.getOwnerUserId())) {
            throw new ForbiddenOperationException("Only the event owner can remove participants");
        }

        accessRequestRepository.findByEventIdAndRequesterUserId(eventId, participantUserId)
                .ifPresent(accessRequest -> {
                    accessRequest.setStatus(ApprovalStatus.REJECTED);
                    accessRequestRepository.save(accessRequest);
                    log.info("🚫 Access request revoked for user {} in event {}", participantUserId, eventId);
                });

        if (event.getCollaborators() != null) {
            boolean removed = event.getCollaborators().removeIf(
                    c -> participantUserId.equals(c.getUserId()));
            if (removed) {
                eventRepository.save(event);
                log.info("🚫 Removed user {} from collaborator list in event {}", participantUserId, eventId);
            }
        }
    }
}
