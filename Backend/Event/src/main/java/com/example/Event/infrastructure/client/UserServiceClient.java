package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.AuthUserLookupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pure declarative Feign Client contract for communicating with Auth Service.
 * Adheres strictly to the Single Responsibility Principle (SRP).
 */
@FeignClient(name = "auth-service", url = "${auth.service.url:http://localhost:8081}")
public interface UserServiceClient {

    @GetMapping("/api/v1/users")
    AuthUserLookupResponse findUser(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "email", required = false) String email
    );
}
