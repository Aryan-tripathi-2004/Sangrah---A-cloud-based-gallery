package com.example.Auth.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events in the Sangrah platform.
 */
public abstract class BaseEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String correlationId;
    private String source;

    protected BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    protected BaseEvent(String eventType, String source) {
        this();
        this.eventType = eventType;
        this.source = source;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
