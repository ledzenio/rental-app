package com.example.rentalservice.api.dto;

import java.math.BigDecimal;

public record EquipmentOptionResponse(
        Long id,
        String inventoryCode,
        String modelName,
        BigDecimal accumulatedWearPercent,
        BigDecimal rentalWearRatePerDay
) {
}
