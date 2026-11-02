package com.example.Event.api.dto.response;

import java.util.List;

public record PendingRequestsEventResponse(
    String id,
    String title,
    long pendingCount
) {}
