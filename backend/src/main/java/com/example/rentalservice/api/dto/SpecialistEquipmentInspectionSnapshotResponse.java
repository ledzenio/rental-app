package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SpecialistEquipmentInspectionSnapshotResponse(
        Long historyId,
        String conditionLabel,
        BigDecimal calculatedWearPercent,
        BigDecimal amortizationValue,
        Instant recordedAt
) {
}
