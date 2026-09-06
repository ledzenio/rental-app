package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.ServiceCatalogItemResponse;
import com.example.rentalservice.api.dto.ServiceCatalogItemImageResponse;
import com.example.rentalservice.api.dto.ServiceCatalogItemSpecResponse;
import com.example.rentalservice.domain.ServiceCatalogItem;
import com.example.rentalservice.domain.ServiceCatalogItemImage;
import com.example.rentalservice.repository.ServiceCatalogRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceCatalogQueryService {

    private final ServiceCatalogRepository serviceCatalogRepository;

    public ServiceCatalogQueryService(ServiceCatalogRepository serviceCatalogRepository) {
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Transactional(readOnly = true)
    public Page<ServiceCatalogItemResponse> list(
            String query,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean active,
            Pageable pageable
    ) {
        Specification<ServiceCatalogItem> spec = Specification.where(null);

        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
            ));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
        }
        if (minPrice != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
        }
        if (active != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("active"), active));
        }

        return serviceCatalogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceCatalogItemResponse getById(Long id) {
        ServiceCatalogItem item = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        return toResponse(item);
    }

    private ServiceCatalogItemResponse toResponse(ServiceCatalogItem item) {
        List<ServiceCatalogItemImageResponse> images = item.getImages()
                .stream()
                .sorted(Comparator.comparing(ServiceCatalogItemImage::getSortOrder).thenComparing(ServiceCatalogItemImage::getId))
                .map(image -> new ServiceCatalogItemImageResponse(
                        image.getImageUrl(),
                        image.getAltText(),
                        image.isCover()
                ))
                .toList();

        String coverImageUrl = images.stream()
                .filter(ServiceCatalogItemImageResponse::cover)
                .map(ServiceCatalogItemImageResponse::imageUrl)
                .findFirst()
                .orElse(images.stream().map(ServiceCatalogItemImageResponse::imageUrl).findFirst().orElse(null));

        List<ServiceCatalogItemSpecResponse> specs = item.getSpecs()
                .stream()
                .map(spec -> new ServiceCatalogItemSpecResponse(spec.getSpecKey(), spec.getSpecValue()))
                .toList();

        return new ServiceCatalogItemResponse(
                item.getId(),
                item.getTitle(),
                item.getSubtitle(),
                item.getDescription(),
                item.getCategory(),
                item.getBasePrice(),
                item.isActive(),
                coverImageUrl,
                specs,
                images
        );
    }
}
