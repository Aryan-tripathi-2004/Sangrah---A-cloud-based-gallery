package com.example.Auth.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDTO {
    private String id;
    private String email;
    private String displayName;
    private String createdAt;
}
