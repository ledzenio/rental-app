package com.example.rentalservice.api.dto;

import com.example.rentalservice.domain.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;

public record AdminRequestStatusUpdate(@NotNull ServiceRequestStatus status) {
}
