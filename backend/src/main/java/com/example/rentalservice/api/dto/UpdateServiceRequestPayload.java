package com.example.rentalservice.api.dto;

import java.time.LocalDate;

public record UpdateServiceRequestPayload(
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        String objectAddress
) {
}
