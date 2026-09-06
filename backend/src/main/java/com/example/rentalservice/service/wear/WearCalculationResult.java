package com.example.rentalservice.service.wear;

import java.math.BigDecimal;

public record WearCalculationResult(
        BigDecimal calculatedWearPercent,
        BigDecimal amortizationValue
) {
}
