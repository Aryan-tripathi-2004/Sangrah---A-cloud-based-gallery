package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Pure declarative Feign Client contract for communicating with Notification Service.
 * Adheres strictly to the Single Responsibility Principle (SRP).
 */
@FeignClient(name = "notification-service", url = "${notification-service.url:http://localhost:8085}")
public interface NotificationServiceClient {

    @PostMapping("/internal/api/v1/notifications")
    String sendNotification(@RequestBody NotificationRequest request);
}
