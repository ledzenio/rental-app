package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.RoleName;
import com.example.rentalservice.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank String password,
        @NotNull UserStatus status,
        @NotNull RoleName role
) {
}
