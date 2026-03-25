package com.example.Email.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendResponse {

    private String status;  // sent, pending, failed

    private String emailId;  // Unique email log ID

    private Instant timestamp;

    private String message;

    private String errorReason;  // If failed
}
