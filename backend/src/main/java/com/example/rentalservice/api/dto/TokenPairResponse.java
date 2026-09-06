package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.RoleName;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String email,
        RoleName role
) {
}
