package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SavedServiceResponse(
        Long serviceId,
        String title,
        String category,
        BigDecimal basePrice,
        Instant savedAt
) {
}
