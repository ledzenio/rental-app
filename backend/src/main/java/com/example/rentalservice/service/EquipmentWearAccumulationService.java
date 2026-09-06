package com.example.rentalservice.service;

import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.Equipment;
import com.example.rentalservice.domain.EquipmentWearLedger;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.domain.WearLedgerEntryType;
import com.example.rentalservice.repository.DefectReportRepository;
import com.example.rentalservice.repository.EquipmentRepository;
import com.example.rentalservice.repository.EquipmentWearLedgerRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentWearAccumulationService {

    private static final BigDecimal MAX_WEAR = new BigDecimal("100.00");

    private final EquipmentWearLedgerRepository wearLedgerRepository;
    private final EquipmentRepository equipmentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final DefectReportRepository defectReportRepository;

    public EquipmentWearAccumulationService(
            EquipmentWearLedgerRepository wearLedgerRepository,
            EquipmentRepository equipmentRepository,
            ServiceRequestRepository serviceRequestRepository,
            DefectReportRepository defectReportRepository
    ) {
        this.wearLedgerRepository = wearLedgerRepository;
        this.equipmentRepository = equipmentRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.defectReportRepository = defectReportRepository;
    }

    /**
     * Начисляет износ за период аренды при переводе заявки в COMPLETED (идемпотентно по заявке).
     */
    @Transactional
    public void applyRentalWearForCompletedRequest(Long serviceRequestId) {
        if (wearLedgerRepository.existsByEntryTypeAndServiceRequest_Id(
                WearLedgerEntryType.RENTAL_CLOSURE,
                serviceRequestId
        )) {
            return;
        }
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId).orElse(null);
        if (sr == null || sr.getStatus() != ServiceRequestStatus.COMPLETED) {
            return;
        }
        Equipment equipment = equipmentRepository.findById(sr.getEquipment().getId()).orElse(null);
        if (equipment == null) {
            return;
        }
        int rentalDays = (int) ChronoUnit.DAYS.between(sr.getRentalStartDate(), sr.getRentalEndDate()) + 1;
        if (rentalDays < 1) {
            rentalDays = 1;
        }
        BigDecimal rate = equipment.getRentalWearRatePerDay() != null
                ? equipment.getRentalWearRatePerDay()
                : new BigDecimal("0.0800");
        BigDecimal rentalDelta = rate
                .multiply(BigDecimal.valueOf(rentalDays))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal before = equipment.getAccumulatedWearPercent() != null
                ? equipment.getAccumulatedWearPercent()
                : BigDecimal.ZERO;
        BigDecimal after = capWear(before.add(rentalDelta));
        equipment.setAccumulatedWearPercent(after);
        equipmentRepository.save(equipment);

        EquipmentWearLedger row = new EquipmentWearLedger();
        row.setEquipment(equipment);
        row.setEntryType(WearLedgerEntryType.RENTAL_CLOSURE);
        row.setServiceRequest(sr);
        row.setRentalDays(rentalDays);
        row.setRentalRatePerDay(rate);
        row.setRentalWearDelta(rentalDelta);
        row.setDefectWearDelta(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        row.setTotalWearDelta(rentalDelta);
        row.setCreatedAt(Instant.now());
        wearLedgerRepository.save(row);
    }

    /**
     * Начисляет доп. износ по утверждённой ведомости (значение зафиксировано при создании ведомости).
     */
    @Transactional
    public void applyDefectWearForApprovedReport(Long defectReportId) {
        if (wearLedgerRepository.existsByEntryTypeAndDefectReport_Id(
                WearLedgerEntryType.DEFECT_APPROVAL,
                defectReportId
        )) {
            return;
        }
        DefectReport report = defectReportRepository.findById(defectReportId).orElse(null);
        if (report == null || report.getStatus() != DefectReportStatus.APPROVED) {
            return;
        }
        Equipment equipment = equipmentRepository.findById(report.getEquipment().getId()).orElse(null);
        if (equipment == null) {
            return;
        }
        BigDecimal defectDelta = report.getPlannedExtraWearPercent() != null
                ? report.getPlannedExtraWearPercent()
                : BigDecimal.ZERO;
        defectDelta = defectDelta.setScale(4, RoundingMode.HALF_UP);
        BigDecimal before = equipment.getAccumulatedWearPercent() != null
                ? equipment.getAccumulatedWearPercent()
                : BigDecimal.ZERO;
        BigDecimal after = capWear(before.add(defectDelta));
        equipment.setAccumulatedWearPercent(after);
        equipmentRepository.save(equipment);

        EquipmentWearLedger row = new EquipmentWearLedger();
        row.setEquipment(equipment);
        row.setEntryType(WearLedgerEntryType.DEFECT_APPROVAL);
        row.setServiceRequest(report.getServiceRequest());
        row.setDefectReport(report);
        row.setRentalDays(null);
        row.setRentalRatePerDay(null);
        row.setRentalWearDelta(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        row.setDefectWearDelta(defectDelta);
        row.setTotalWearDelta(defectDelta);
        row.setCreatedAt(Instant.now());
        wearLedgerRepository.save(row);
    }

    private static BigDecimal capWear(BigDecimal value) {
        BigDecimal v = value.setScale(2, RoundingMode.HALF_UP);
        if (v.compareTo(MAX_WEAR) > 0) {
            return MAX_WEAR;
        }
        if (v.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v;
    }
}
