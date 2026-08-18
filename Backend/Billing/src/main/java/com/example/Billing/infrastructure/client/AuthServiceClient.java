package com.example.Billing.infrastructure.client;

import com.example.Billing.infrastructure.client.dto.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service-billing", url = "${auth.service.url:http://localhost:8081}")
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{userId}")
    AuthUserResponse getUserById(@PathVariable("userId") String userId);
}
