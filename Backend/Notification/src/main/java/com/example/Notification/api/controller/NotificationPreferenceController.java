package com.example.Notification.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
public class NotificationPreferenceController {

    @GetMapping
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<Map<String, Boolean>> get() {
        return ResponseEntity.ok(Map.of("inAppEnabled", true, "emailEnabled", false));
    }

    @PatchMapping
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<Map<String, Boolean>> update(@RequestBody Map<String, Boolean> payload) {
        return ResponseEntity.ok(payload);
    }
}
