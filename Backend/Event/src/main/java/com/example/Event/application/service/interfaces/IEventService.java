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
import com.example.Event.shared.enums.EventScope;
import com.example.Event.shared.enums.EventVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IEventService {
    EventCreateResponse createEvent(EventRequest request, MultipartFile coverMedia, String userId);

    default EventCreateResponse createEvent(EventRequest request, String userId) {
        return createEvent(request, null, userId);
    }

    List<EventSummaryResponse> getGlobalEvents(String userId);

    List<EventSummaryResponse> getMyEvents(String userId);

    EventResponse getEvent(String eventId, String userId);

    EventResponse updateEvent(
            String eventId,
            EventUpdateRequest eventDetails,
            MultipartFile coverMedia,
            String userId);

    EventDeleteResponse deleteEvent(String eventId, String userId);

    EventSettingsResponse getSettings(String eventId);

    EventSettingsResponse updateSettings(String eventId, EventSettingsRequest request, String userId);

    List<EventSummaryResponse> getEventsWithPendingRequests(String userId);

    default List<EventSummaryResponse> getEvents(String userId, EventScope scope) {
        EventScope targetScope = scope != null ? scope : EventScope.GLOBAL;
        return switch (targetScope) {
            case MY_EVENTS -> getMyEvents(userId);
            case PENDING_REQUESTS -> getEventsWithPendingRequests(userId);
            default -> getGlobalEvents(userId);
        };
    }

    Page<EventSummaryResponse> getGlobalEvents(String userId, Pageable pageable);

    Page<EventSummaryResponse> getGlobalEvents(String userId, EventVisibility visibility, Pageable pageable);

    Page<EventSummaryResponse> getMyEvents(String userId, Pageable pageable);

    Page<EventSummaryResponse> getEventsWithPendingRequests(String userId, Pageable pageable);

    default Page<EventSummaryResponse> getEvents(String userId, EventScope scope, Pageable pageable) {
        return getEvents(userId, scope, null, pageable);
    }

    default Page<EventSummaryResponse> getEvents(String userId, EventScope scope, EventVisibility visibility, Pageable pageable) {
        EventScope targetScope = scope != null ? scope : EventScope.GLOBAL;
        return switch (targetScope) {
            case MY_EVENTS -> getMyEvents(userId, pageable);
            case PENDING_REQUESTS -> getEventsWithPendingRequests(userId, pageable);
            default -> getGlobalEvents(userId, visibility, pageable);
        };
    }
}
