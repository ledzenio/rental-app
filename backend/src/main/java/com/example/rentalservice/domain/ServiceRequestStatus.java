package com.example.rentalservice.domain;

public enum ServiceRequestStatus {
    NEW,
    AWAITING_PAYMENT,
    IN_PROGRESS,
    AWAITING_SPECIALIST_REVIEW,
    COMPLETED,
    CANCELLED
}
