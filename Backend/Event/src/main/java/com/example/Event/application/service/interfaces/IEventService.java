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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IEventService {
    EventCreateResponse createEvent(EventRequest request, HttpServletRequest httpRequest);

    List<EventSummaryResponse> getGlobalEvents(HttpServletRequest httpRequest);

    List<EventSummaryResponse> getMyEvents(HttpServletRequest httpRequest);

    EventResponse getEvent(String eventId, HttpServletRequest httpRequest);

    EventUpdateResponse updateEvent(
            String eventId,
            EventUpdateRequest eventDetails,
            MultipartFile coverMedia,
            HttpServletRequest httpRequest);

    EventDeleteResponse deleteEvent(String eventId, HttpServletRequest httpRequest);

    EventSettingsResponse getSettings(String eventId);

    EventSettingsResponse updateSettings(String eventId, EventSettingsRequest request, HttpServletRequest httpRequest);

    EventDocument getEventById(String eventId);

    List<EventDocument> getEventsByOwner(String ownerUserId);

    List<EventDocument> getPublicEvents();

    List<EventDocument> getProtectedEvents();

    EventDocument updateEventDirectly(EventDocument event);
}
