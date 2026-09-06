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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "equipment_state_history")
public class EquipmentStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "condition_label", nullable = false, length = 64)
    private String conditionLabel;

    @Column(name = "battery_percent")
    private Short batteryPercent;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "calculated_wear_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal calculatedWearPercent;

    @Column(name = "amortization_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal amortizationValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public void setConditionLabel(String conditionLabel) {
        this.conditionLabel = conditionLabel;
    }

    public Short getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(Short batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public BigDecimal getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(BigDecimal temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public BigDecimal getCalculatedWearPercent() {
        return calculatedWearPercent;
    }

    public void setCalculatedWearPercent(BigDecimal calculatedWearPercent) {
        this.calculatedWearPercent = calculatedWearPercent;
    }

    public BigDecimal getAmortizationValue() {
        return amortizationValue;
    }

    public void setAmortizationValue(BigDecimal amortizationValue) {
        this.amortizationValue = amortizationValue;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
