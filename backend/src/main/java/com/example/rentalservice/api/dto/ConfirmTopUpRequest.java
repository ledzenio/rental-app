package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConfirmTopUpRequest(
        @NotNull @DecimalMin(value = "1.00", message = "Amount must be at least 1.00") BigDecimal amount,
        @NotBlank String code
) {
}
