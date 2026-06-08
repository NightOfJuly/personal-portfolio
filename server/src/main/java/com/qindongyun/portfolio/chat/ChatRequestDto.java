package com.qindongyun.portfolio.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequestDto(
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "sessionId must be a UUID")
        String sessionId,

        @NotBlank
        @Size(max = 500, message = "message must not exceed 500 characters")
        String message) {
}

