package com.example.Gallery.api.dto.response;

public record QuotaResponse(String message, long limitBytes, long usedBytes, boolean deprecated, String useInstead) {
}
