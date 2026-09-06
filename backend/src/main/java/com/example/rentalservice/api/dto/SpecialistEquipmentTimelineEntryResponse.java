package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SpecialistEquipmentTimelineEntryResponse(
        SpecialistEquipmentStateEventType eventType,
        Instant occurredAt,
        String title,
        String description,
        Long stateHistoryId,
        Long wearLedgerId,
        Long serviceRequestId,
        Long defectReportId,
        BigDecimal inspectionCalculatedWearPercent,
        BigDecimal rentalWearDelta,
        BigDecimal defectWearDelta,
        Integer rentalDays,
        BigDecimal rentalRatePerDay
) {
}
