package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.RoleName;
import com.example.rentalservice.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRequest(
        @NotNull @Email String email,
        @NotNull String fullName,
        String password,
        @NotNull UserStatus status,
        @NotNull RoleName role
) {
}
