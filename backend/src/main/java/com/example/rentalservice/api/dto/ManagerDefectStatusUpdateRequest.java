package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.DefectReportStatus;
import jakarta.validation.constraints.NotNull;

public record ManagerDefectStatusUpdateRequest(@NotNull DefectReportStatus status) {
}
