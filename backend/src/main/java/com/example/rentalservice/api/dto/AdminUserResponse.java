package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.RoleName;
import com.example.rentalservice.domain.UserStatus;
import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        UserStatus status,
        RoleName role,
        Instant createdAt
) {
}
