package com.example.rentalservice.service.wear;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class DefaultWearCalculationStrategy implements WearCalculationStrategy {

    @Override
    public WearCalculationResult calculate(WearCalculationInput input) {
        BigDecimal wear = input.baseWearPercent();
        wear = wear.add(conditionFactor(input.conditionLabel()));
        wear = wear.add(temperatureFactor(input.temperatureCelsius()));
        wear = wear.add(batteryFactor(input.batteryPercent()));
        wear = wear.add(operatingHoursFactor(input.operatingHoursDelta()));
        if (wear.compareTo(BigDecimal.valueOf(100)) > 0) {
            wear = BigDecimal.valueOf(100);
        }
        wear = wear.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amortization = input.purchasePrice()
                .multiply(wear)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new WearCalculationResult(wear, amortization);
    }

    private BigDecimal conditionFactor(String conditionLabel) {
        String value = conditionLabel == null ? "" : conditionLabel.trim().toUpperCase();
        return switch (value) {
            case "ОТЛИЧНОЕ", "EXCELLENT" -> BigDecimal.ZERO;
            case "ПОСЛЕ РЕМОНТА", "POST_REPAIR" -> BigDecimal.ZERO;
            case "ХОРОШЕЕ", "GOOD" -> BigDecimal.valueOf(1.5);
            case "УДОВЛЕТВОРИТЕЛЬНОЕ", "FAIR" -> BigDecimal.valueOf(3.0);
            case "ПЛОХОЕ", "POOR" -> BigDecimal.valueOf(6.0);
            case "АВАРИЙНОЕ", "DAMAGED" -> BigDecimal.valueOf(12.0);
            default -> BigDecimal.valueOf(2.5);
        };
    }

    private BigDecimal temperatureFactor(BigDecimal temperature) {
        if (temperature == null) {
            return BigDecimal.ZERO;
        }
        if (temperature.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return BigDecimal.valueOf(4.0);
        }
        if (temperature.compareTo(BigDecimal.valueOf(45)) >= 0) {
            return BigDecimal.valueOf(2.0);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal batteryFactor(Short batteryPercent) {
        if (batteryPercent == null) {
            return BigDecimal.ZERO;
        }
        if (batteryPercent < 20) {
            return BigDecimal.valueOf(2.5);
        }
        if (batteryPercent < 40) {
            return BigDecimal.valueOf(1.0);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal operatingHoursFactor(Integer operatingHoursDelta) {
        if (operatingHoursDelta == null || operatingHoursDelta <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(operatingHoursDelta)
                .multiply(BigDecimal.valueOf(0.03))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
