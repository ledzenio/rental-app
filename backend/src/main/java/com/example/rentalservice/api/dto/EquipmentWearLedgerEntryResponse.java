package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.WearLedgerEntryType;
import java.math.BigDecimal;
import java.time.Instant;

public record EquipmentWearLedgerEntryResponse(
        Long id,
        WearLedgerEntryType entryType,
        Long serviceRequestId,
        Long defectReportId,
        Integer rentalDays,
        BigDecimal rentalRatePerDay,
        BigDecimal rentalWearDelta,
        BigDecimal defectWearDelta,
        BigDecimal totalWearDelta,
        Instant createdAt
) {
}
