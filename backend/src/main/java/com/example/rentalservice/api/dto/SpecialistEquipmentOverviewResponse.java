package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.EquipmentLifecycleStatus;
import java.math.BigDecimal;

public record SpecialistEquipmentOverviewResponse(
        Long equipmentId,
        String inventoryCode,
        String modelName,
        String category,
        String catalogTitle,
        EquipmentLifecycleStatus lifecycleStatus,
        BigDecimal accumulatedWearPercent,
        BigDecimal rentalWearRatePerDay,
        SpecialistEquipmentInspectionSnapshotResponse latestInspection
) {
}
