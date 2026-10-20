package com.example.Billing.infrastructure.client;

import com.example.Billing.infrastructure.client.dto.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service-billing", url = "${auth.service.url:http://localhost:8081}")
public interface AuthServiceClient {

    @GetMapping("/api/v1/users")
    AuthUserResponse getUserById(@RequestParam("id") String userId);
}
