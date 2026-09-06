package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EquipmentRepairAlertResponse(
        Long equipmentId,
        String inventoryCode,
        String modelName,
        BigDecimal accumulatedWearPercent,
        Long latestHistoryId,
        String latestConditionLabel,
        BigDecimal latestWearPercent,
        BigDecimal latestAmortizationValue,
        Instant latestRecordedAt
) {
}
