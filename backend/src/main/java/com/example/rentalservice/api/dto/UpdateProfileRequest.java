package com.example.rentalservice.api.dto;

public record UpdateProfileRequest(
        String email,
        String fullName,
        String phoneNumber
) {
}
