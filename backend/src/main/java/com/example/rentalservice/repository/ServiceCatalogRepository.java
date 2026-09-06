package com.example.rentalservice.repository;

import com.example.rentalservice.domain.ServiceCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogItem, Long>, JpaSpecificationExecutor<ServiceCatalogItem> {
}
