package com.example.Event.api.dto.response;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

public record EventMediaFileResponse(
        ByteArrayResource content,
        MediaType contentType
) {
}
