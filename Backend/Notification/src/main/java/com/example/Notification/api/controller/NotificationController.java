package com.example.Notification.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @GetMapping
    @Operation(summary = "List notifications")
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable String notificationId) {
        return ResponseEntity.ok(Map.of("notificationId", notificationId, "read", true));
    }
}
