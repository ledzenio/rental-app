package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.DefectSeverity;
import java.math.BigDecimal;
import java.time.Instant;

public record DefectReportResponse(
        Long id,
        Long equipmentId,
        String equipmentInventoryCode,
        String equipmentModelName,
        Long specialistUserId,
        Long serviceRequestId,
        Long penaltyInvoiceId,
        String penaltyInvoiceStatus,
        String defectDescription,
        DefectSeverity severity,
        BigDecimal recommendedPenalty,
        BigDecimal plannedExtraWearPercent,
        DefectReportStatus status,
        Instant createdAt
) {
}
