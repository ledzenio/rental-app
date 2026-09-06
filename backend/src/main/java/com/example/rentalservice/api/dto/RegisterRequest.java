package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullName
) {
}
