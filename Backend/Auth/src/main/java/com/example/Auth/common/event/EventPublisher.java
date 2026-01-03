package com.example.Auth.common.event;

/**
 * Interface for event publishers.
 */
public interface EventPublisher {

    /**
     * Publish an event.
     *
     * @param event the event to publish
     */
    void publish(BaseEvent event);

    /**
     * Publish an event to a specific exchange/topic.
     *
     * @param exchange   the exchange or topic name
     * @param routingKey the routing key
     * @param event      the event to publish
     */
    void publish(String exchange, String routingKey, BaseEvent event);
}
