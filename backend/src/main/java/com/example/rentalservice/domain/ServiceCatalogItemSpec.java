package com.example.rentalservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_catalog_item_specs")
public class ServiceCatalogItemSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceCatalogItem service;

    @Column(name = "spec_key", nullable = false, length = 120)
    private String specKey;

    @Column(name = "spec_value", nullable = false, length = 255)
    private String specValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public ServiceCatalogItem getService() {
        return service;
    }

    public void setService(ServiceCatalogItem service) {
        this.service = service;
    }

    public String getSpecKey() {
        return specKey;
    }

    public void setSpecKey(String specKey) {
        this.specKey = specKey;
    }

    public String getSpecValue() {
        return specValue;
    }

    public void setSpecValue(String specValue) {
        this.specValue = specValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
