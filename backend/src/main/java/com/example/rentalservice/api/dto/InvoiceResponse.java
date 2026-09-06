package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.InvoiceStatus;
import com.example.rentalservice.domain.InvoiceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InvoiceResponse(
        Long id,
        Long userId,
        Long serviceRequestId,
        Long defectReportId,
        InvoiceType invoiceType,
        BigDecimal amount,
        String currency,
        InvoiceStatus status,
        String description,
        Instant createdAt,
        Instant paidAt,
        String serviceTitle,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate
) {
}
