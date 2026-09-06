package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ManagerServiceCatalogImageUpsertRequest(
        @NotBlank String imageUrl,
        String altText,
        boolean cover
) {
}
