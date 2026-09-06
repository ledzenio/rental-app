package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ServiceCatalogItemResponse(
        Long id,
        String title,
        String subtitle,
        String description,
        String category,
        BigDecimal basePrice,
        boolean active,
        String coverImageUrl,
        List<ServiceCatalogItemSpecResponse> specs,
        List<ServiceCatalogItemImageResponse> images
) {
}
