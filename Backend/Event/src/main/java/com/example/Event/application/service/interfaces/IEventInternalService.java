package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.response.EventInternalDTO;

import java.util.List;

public interface IEventInternalService {
    EventInternalDTO getEventById(String eventId);
    List<EventInternalDTO> getEventsByOwner(String ownerUserId);
    List<EventInternalDTO> getPublicEvents();
    List<EventInternalDTO> getProtectedEvents();
}
