package com.example.rentalservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "equipment_wear_ledger")
public class EquipmentWearLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 32)
    private WearLedgerEntryType entryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_report_id")
    private DefectReport defectReport;

    @Column(name = "rental_days")
    private Integer rentalDays;

    @Column(name = "rental_rate_per_day", precision = 7, scale = 4)
    private BigDecimal rentalRatePerDay;

    @Column(name = "rental_wear_delta", nullable = false, precision = 8, scale = 4)
    private BigDecimal rentalWearDelta = BigDecimal.ZERO;

    @Column(name = "defect_wear_delta", nullable = false, precision = 8, scale = 4)
    private BigDecimal defectWearDelta = BigDecimal.ZERO;

    @Column(name = "total_wear_delta", nullable = false, precision = 8, scale = 4)
    private BigDecimal totalWearDelta;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public WearLedgerEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(WearLedgerEntryType entryType) {
        this.entryType = entryType;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public DefectReport getDefectReport() {
        return defectReport;
    }

    public void setDefectReport(DefectReport defectReport) {
        this.defectReport = defectReport;
    }

    public Integer getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(Integer rentalDays) {
        this.rentalDays = rentalDays;
    }

    public BigDecimal getRentalRatePerDay() {
        return rentalRatePerDay;
    }

    public void setRentalRatePerDay(BigDecimal rentalRatePerDay) {
        this.rentalRatePerDay = rentalRatePerDay;
    }

    public BigDecimal getRentalWearDelta() {
        return rentalWearDelta;
    }

    public void setRentalWearDelta(BigDecimal rentalWearDelta) {
        this.rentalWearDelta = rentalWearDelta;
    }

    public BigDecimal getDefectWearDelta() {
        return defectWearDelta;
    }

    public void setDefectWearDelta(BigDecimal defectWearDelta) {
        this.defectWearDelta = defectWearDelta;
    }

    public BigDecimal getTotalWearDelta() {
        return totalWearDelta;
    }

    public void setTotalWearDelta(BigDecimal totalWearDelta) {
        this.totalWearDelta = totalWearDelta;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
