package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateServiceRequestPayload(
        @NotNull Long serviceId,
        @NotNull LocalDate rentalStartDate,
        @NotNull LocalDate rentalEndDate,
        String notes,
        @NotBlank String objectAddress
) {
}
