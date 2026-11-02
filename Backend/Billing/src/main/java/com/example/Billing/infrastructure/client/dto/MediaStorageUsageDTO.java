package com.example.Billing.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaStorageUsageDTO(
        String userId,
        Long usedBytes,
        Long limitBytes,
        Double percentageUsed
) {}
