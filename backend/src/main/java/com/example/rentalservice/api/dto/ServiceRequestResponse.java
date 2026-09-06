package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.ServiceRequestStatus;
import java.time.Instant;
import java.time.LocalDate;

public record ServiceRequestResponse(
        Long requestId,
        Long serviceId,
        String serviceTitle,
        Long equipmentId,
        String equipmentInventoryCode,
        String equipmentModelName,
        Long userId,
        String userEmail,
        java.math.BigDecimal serviceBasePrice,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        Integer rentalDays,
        Instant returnedAt,
        String notes,
        ServiceRequestStatus status,
        Instant createdAt,
        boolean specialistConclusionRecorded
) {
}
