package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.request.EventSettingsRequest;
import com.example.Event.api.dto.request.EventUpdateRequest;
import com.example.Event.api.dto.response.EventCreateResponse;
import com.example.Event.api.dto.response.EventDeleteResponse;
import com.example.Event.api.dto.response.EventResponse;
import com.example.Event.api.dto.response.EventSettingsResponse;
import com.example.Event.api.dto.response.EventSummaryResponse;
import com.example.Event.api.dto.response.EventUpdateResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IEventService {
    EventCreateResponse createEvent(EventRequest request, String userId);

    List<EventSummaryResponse> getGlobalEvents(String userId);

    List<EventSummaryResponse> getMyEvents(String userId);

    EventResponse getEvent(String eventId, String userId);

    EventUpdateResponse updateEvent(
            String eventId,
            EventUpdateRequest eventDetails,
            MultipartFile coverMedia,
            String userId);

    EventDeleteResponse deleteEvent(String eventId, String userId);

    EventSettingsResponse getSettings(String eventId);

    EventSettingsResponse updateSettings(String eventId, EventSettingsRequest request, String userId);

    EventDocument getEventById(String eventId);

    List<EventDocument> getEventsByOwner(String ownerUserId);

    List<EventDocument> getPublicEvents();

    List<EventDocument> getProtectedEvents();

    EventDocument updateEventDirectly(EventDocument event);
}
