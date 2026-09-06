package com.example.rentalservice.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ManagerServiceCatalogUpsertRequest(
        @NotBlank String title,
        String subtitle,
        @NotBlank String description,
        @NotBlank String category,
        @NotNull @DecimalMin("0.0") BigDecimal basePrice,
        @NotNull Boolean active,
        @Valid List<ManagerServiceCatalogSpecUpsertRequest> specs,
        @Valid List<ManagerServiceCatalogImageUpsertRequest> images
) {
}
