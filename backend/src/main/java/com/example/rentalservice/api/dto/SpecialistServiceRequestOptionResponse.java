package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SpecialistServiceRequestOptionResponse(
        Long requestId,
        Long serviceId,
        String serviceTitle,
        String userEmail,
        Long equipmentId,
        String equipmentInventoryCode,
        String equipmentModelName,
        EquipmentSnapshot latestEquipmentState
) {
    public record EquipmentSnapshot(
            Long historyId,
            String conditionLabel,
            BigDecimal calculatedWearPercent,
            BigDecimal amortizationValue,
            Instant recordedAt
    ) {
    }
}
