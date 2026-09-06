package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ManagerServiceCatalogSpecUpsertRequest(
        @NotBlank String key,
        @NotBlank String value
) {
}
