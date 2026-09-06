package com.example.rentalservice.service.wear;

import com.example.rentalservice.domain.DefectSeverity;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;


public final class DefectWearBonusPolicy {

    private static final Map<DefectSeverity, BigDecimal> EXTRA_WEAR_BY_SEVERITY = new EnumMap<>(DefectSeverity.class);

    static {
        EXTRA_WEAR_BY_SEVERITY.put(DefectSeverity.LOW, new BigDecimal("0.4000"));
        EXTRA_WEAR_BY_SEVERITY.put(DefectSeverity.MEDIUM, new BigDecimal("1.1000"));
        EXTRA_WEAR_BY_SEVERITY.put(DefectSeverity.HIGH, new BigDecimal("2.2000"));
        EXTRA_WEAR_BY_SEVERITY.put(DefectSeverity.CRITICAL, new BigDecimal("4.5000"));
    }

    private DefectWearBonusPolicy() {
    }

    public static BigDecimal plannedExtraWearForSeverity(DefectSeverity severity) {
        if (severity == null) {
            return BigDecimal.ZERO;
        }
        return EXTRA_WEAR_BY_SEVERITY.getOrDefault(severity, BigDecimal.ZERO);
    }
}
