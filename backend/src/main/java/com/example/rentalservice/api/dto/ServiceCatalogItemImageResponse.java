package com.example.rentalservice.api.dto;

public record ServiceCatalogItemImageResponse(
        String imageUrl,
        String altText,
        boolean cover
) {
}
