package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EquipmentStateRecordedResponse(
        Long historyId,
        Long equipmentId,
        String conditionLabel,
        BigDecimal calculatedWearPercent,
        BigDecimal amortizationValue,
        Instant recordedAt,
        boolean criticalConditionDetected
) {
}
