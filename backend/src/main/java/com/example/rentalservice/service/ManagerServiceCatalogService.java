package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.ManagerServiceCatalogUpsertRequest;
import com.example.rentalservice.api.dto.ManagerServiceCatalogImageUpsertRequest;
import com.example.rentalservice.api.dto.ManagerServiceCatalogSpecUpsertRequest;
import com.example.rentalservice.api.dto.ServiceCatalogItemResponse;
import com.example.rentalservice.api.dto.ServiceCatalogItemImageResponse;
import com.example.rentalservice.api.dto.ServiceCatalogItemSpecResponse;
import com.example.rentalservice.domain.ServiceCatalogItem;
import com.example.rentalservice.domain.ServiceCatalogItemImage;
import com.example.rentalservice.domain.ServiceCatalogItemSpec;
import com.example.rentalservice.repository.ServiceCatalogRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;

    public ManagerServiceCatalogService(ServiceCatalogRepository serviceCatalogRepository) {
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Transactional
    public ServiceCatalogItemResponse create(ManagerServiceCatalogUpsertRequest payload) {
        ServiceCatalogItem item = new ServiceCatalogItem();
        apply(item, payload);
        item.setCreatedAt(Instant.now());
        return toResponse(serviceCatalogRepository.save(item));
    }

    @Transactional
    public ServiceCatalogItemResponse update(Long id, ManagerServiceCatalogUpsertRequest payload) {
        ServiceCatalogItem item = load(id);
        apply(item, payload);
        return toResponse(serviceCatalogRepository.save(item));
    }

    @Transactional
    public ServiceCatalogItemResponse setActive(Long id, boolean active) {
        ServiceCatalogItem item = load(id);
        item.setActive(active);
        return toResponse(serviceCatalogRepository.save(item));
    }

    private ServiceCatalogItem load(Long id) {
        return serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    private void apply(ServiceCatalogItem item, ManagerServiceCatalogUpsertRequest payload) {
        item.setTitle(payload.title().trim());
        item.setSubtitle(payload.subtitle() == null ? null : payload.subtitle().trim());
        item.setDescription(payload.description().trim());
        item.setCategory(payload.category().trim());
        item.setBasePrice(payload.basePrice());
        item.setActive(payload.active());
        item.getSpecs().clear();
        List<ManagerServiceCatalogSpecUpsertRequest> specs = payload.specs() == null ? List.of() : payload.specs();
        specs.forEach(spec -> {
            ServiceCatalogItemSpec dbSpec = new ServiceCatalogItemSpec();
            dbSpec.setService(item);
            dbSpec.setSpecKey(spec.key().trim());
            dbSpec.setSpecValue(spec.value().trim());
            dbSpec.setSortOrder(item.getSpecs().size());
            item.getSpecs().add(dbSpec);
        });
        item.getImages().clear();
        List<ManagerServiceCatalogImageUpsertRequest> images = payload.images() == null ? List.of() : payload.images();
        images.forEach(image -> {
            ServiceCatalogItemImage dbImage = new ServiceCatalogItemImage();
            dbImage.setService(item);
            dbImage.setImageUrl(image.imageUrl().trim());
            dbImage.setAltText(image.altText() == null ? null : image.altText().trim());
            dbImage.setSortOrder(item.getImages().size());
            dbImage.setCover(image.cover());
            item.getImages().add(dbImage);
        });
    }

    private ServiceCatalogItemResponse toResponse(ServiceCatalogItem item) {
        String coverImageUrl = item.getImages().stream()
                .filter(ServiceCatalogItemImage::isCover)
                .map(ServiceCatalogItemImage::getImageUrl)
                .findFirst()
                .orElse(item.getImages().stream().map(ServiceCatalogItemImage::getImageUrl).findFirst().orElse(null));
        return new ServiceCatalogItemResponse(
                item.getId(),
                item.getTitle(),
                item.getSubtitle(),
                item.getDescription(),
                item.getCategory(),
                item.getBasePrice(),
                item.isActive(),
                coverImageUrl,
                item.getSpecs().stream().map(x -> new ServiceCatalogItemSpecResponse(
                        x.getSpecKey(),
                        x.getSpecValue()
                )).toList(),
                item.getImages().stream().map(x -> new ServiceCatalogItemImageResponse(
                        x.getImageUrl(),
                        x.getAltText(),
                        x.isCover()
                )).toList()
        );
    }
}
