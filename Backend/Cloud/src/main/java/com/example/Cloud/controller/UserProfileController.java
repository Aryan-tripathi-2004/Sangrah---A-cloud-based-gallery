package com.example.Cloud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cloud")
public class UserProfileController {

    /**
     * Returns basic profile information for the authenticated user.
     *
     * <p>Previously this endpoint returned 403 Forbidden because the security
     * configuration did not correctly propagate the JWT-derived authentication
     * into the security context before the request reached the controller. The
     * {@link com.example.Cloud.filter.JwtAuthenticationFilter} now sets the
     * {@code SecurityContextHolder} so that {@code @AuthenticationPrincipal}
     * correctly resolves the authenticated user, and the endpoint returns 200
     * for any request bearing a valid JWT.</p>
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "email", userDetails.getUsername(),
                "authorities", userDetails.getAuthorities().toString()
        ));
    }
}
