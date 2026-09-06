package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNoDefectsConclusionRequest(
        Long equipmentId,
        @NotNull Long serviceRequestId,
        @NotBlank String conclusionText
) {
}
