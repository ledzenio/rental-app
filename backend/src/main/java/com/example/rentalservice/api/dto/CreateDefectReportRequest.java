package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.DefectSeverity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateDefectReportRequest(
        Long equipmentId,
        Long serviceRequestId,
        @NotBlank String defectDescription,
        @NotNull DefectSeverity severity,
        @NotNull @DecimalMin("0.0") BigDecimal recommendedPenalty
) {
}
