package com.example.Auth.api.controller;

import com.example.Auth.api.dto.response.UserWrapperResponse;
import com.example.Auth.application.service.interfaces.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-only endpoint for inter-service user resolution (Billing, Event).
 * Gateway perimeter blocks all external ingress under /internal/**.
 */
@Slf4j
@RestController
@RequestMapping("/internal/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Internal Users", description = "Internal service APIs for user lookups")
public class InternalUserController {

    private final IUserService userService;

    @GetMapping
    @Operation(summary = "Find user by identifier (internal services only)")
    public ResponseEntity<UserWrapperResponse> findUser(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String email) {
        log.info("🔍 [INTERNAL] Fetching user data for id: {} or email: {}", id, email);
        return ResponseEntity.ok(userService.findUserWrapper(id, email));
    }
}
