package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record RecordEquipmentStateRequest(
        @NotBlank String conditionLabel,
        @Min(0) @Max(100) Short batteryPercent,
        BigDecimal temperatureCelsius,
        @Min(0) Integer operatingHoursDelta,
        String notes
) {
}
