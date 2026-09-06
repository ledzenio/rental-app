package com.example.rentalservice.service.wear;

import java.math.BigDecimal;

public record WearCalculationInput(
        BigDecimal baseWearPercent,
        BigDecimal purchasePrice,
        String conditionLabel,
        Short batteryPercent,
        BigDecimal temperatureCelsius,
        Integer operatingHoursDelta
) {
}
