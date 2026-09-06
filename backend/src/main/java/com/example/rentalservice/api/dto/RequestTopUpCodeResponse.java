package com.example.rentalservice.api.dto;

import java.time.Instant;

public record RequestTopUpCodeResponse(
        String message,
        Instant expiresAt
) {
}
