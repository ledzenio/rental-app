package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.InvoiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateInvoiceRequest(
        @NotNull Long userId,
        Long serviceRequestId,
        Long defectReportId,
        @NotNull InvoiceType invoiceType,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotBlank String description
) {
}
