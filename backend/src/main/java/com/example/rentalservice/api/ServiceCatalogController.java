package com.example.rentalservice.api;

import com.example.rentalservice.api.dto.ServiceCatalogItemResponse;
import com.example.rentalservice.service.ServiceCatalogQueryService;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceCatalogController {

    private final ServiceCatalogQueryService serviceCatalogQueryService;

    public ServiceCatalogController(ServiceCatalogQueryService serviceCatalogQueryService) {
        this.serviceCatalogQueryService = serviceCatalogQueryService;
    }

    @GetMapping
    public Page<ServiceCatalogItemResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return serviceCatalogQueryService.list(query, category, minPrice, maxPrice, active, pageable);
    }

    @GetMapping("/{id}")
    public ServiceCatalogItemResponse getById(@PathVariable Long id) {
        return serviceCatalogQueryService.getById(id);
    }
}
