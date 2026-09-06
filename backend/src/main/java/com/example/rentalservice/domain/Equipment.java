package com.example.rentalservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_code", nullable = false, length = 80)
    private String inventoryCode;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "purchase_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "base_wear_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseWearPercent;

    @ManyToOne
    @JoinColumn(name = "service_catalog_item_id")
    private ServiceCatalogItem serviceCatalogItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private EquipmentLifecycleStatus lifecycleStatus;

    @Column(name = "accumulated_wear_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal accumulatedWearPercent = BigDecimal.ZERO;

    /** Доля накопительного износ за одну календарную сутку аренды (%), индивидуально на единицу. */
    @Column(name = "rental_wear_rate_per_day", nullable = false, precision = 7, scale = 4)
    private BigDecimal rentalWearRatePerDay = new BigDecimal("0.0800");

    public Long getId() {
        return id;
    }

    public String getInventoryCode() {
        return inventoryCode;
    }

    public String getModelName() {
        return modelName;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public BigDecimal getBaseWearPercent() {
        return baseWearPercent;
    }

    public ServiceCatalogItem getServiceCatalogItem() {
        return serviceCatalogItem;
    }

    public void setServiceCatalogItem(ServiceCatalogItem serviceCatalogItem) {
        this.serviceCatalogItem = serviceCatalogItem;
    }

    public EquipmentLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(EquipmentLifecycleStatus lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public BigDecimal getAccumulatedWearPercent() {
        return accumulatedWearPercent;
    }

    public void setAccumulatedWearPercent(BigDecimal accumulatedWearPercent) {
        this.accumulatedWearPercent = accumulatedWearPercent;
    }

    public BigDecimal getRentalWearRatePerDay() {
        return rentalWearRatePerDay;
    }

    public void setRentalWearRatePerDay(BigDecimal rentalWearRatePerDay) {
        this.rentalWearRatePerDay = rentalWearRatePerDay;
    }
}
