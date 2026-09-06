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
@Table(name = "defect_reports")
public class DefectReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_user_id", nullable = false)
    private User specialist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    @Column(name = "defect_description", nullable = false, columnDefinition = "TEXT")
    private String defectDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DefectSeverity severity;

    @Column(name = "recommended_penalty", nullable = false, precision = 12, scale = 2)
    private BigDecimal recommendedPenalty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DefectReportStatus status;

    /**
     * Доп. накопительный износ (%), который будет применён к оборудованию при утверждении ведомости.
     * Для заключения «дефекты не выявлены» — 0.
     */
    @Column(name = "planned_extra_wear_percent", nullable = false, precision = 6, scale = 4)
    private BigDecimal plannedExtraWearPercent = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public User getSpecialist() {
        return specialist;
    }

    public void setSpecialist(User specialist) {
        this.specialist = specialist;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public String getDefectDescription() {
        return defectDescription;
    }

    public void setDefectDescription(String defectDescription) {
        this.defectDescription = defectDescription;
    }

    public DefectSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(DefectSeverity severity) {
        this.severity = severity;
    }

    public BigDecimal getRecommendedPenalty() {
        return recommendedPenalty;
    }

    public void setRecommendedPenalty(BigDecimal recommendedPenalty) {
        this.recommendedPenalty = recommendedPenalty;
    }

    public DefectReportStatus getStatus() {
        return status;
    }

    public void setStatus(DefectReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getPlannedExtraWearPercent() {
        return plannedExtraWearPercent;
    }

    public void setPlannedExtraWearPercent(BigDecimal plannedExtraWearPercent) {
        this.plannedExtraWearPercent = plannedExtraWearPercent;
    }
}
