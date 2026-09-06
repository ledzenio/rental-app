package com.example.rentalservice.api.dto;

public record GeocodingEstimateResponse(
        String inputAddress,
        String normalizedAddress,
        double latitude,
        double longitude,
        double distanceKm,
        double logisticsSurcharge
) {
}
