package com.example.rentalservice.service;

import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.Invoice;
import com.example.rentalservice.domain.InvoiceStatus;
import com.example.rentalservice.domain.InvoiceType;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.repository.DefectReportRepository;
import com.example.rentalservice.repository.InvoiceRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRequestCompletionService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final DefectReportRepository defectReportRepository;
    private final InvoiceRepository invoiceRepository;
    private final EquipmentWearAccumulationService equipmentWearAccumulationService;

    public ServiceRequestCompletionService(
            ServiceRequestRepository serviceRequestRepository,
            DefectReportRepository defectReportRepository,
            InvoiceRepository invoiceRepository,
            EquipmentWearAccumulationService equipmentWearAccumulationService
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.defectReportRepository = defectReportRepository;
        this.invoiceRepository = invoiceRepository;
        this.equipmentWearAccumulationService = equipmentWearAccumulationService;
    }

    /**
     * Завершает заявку после возврата оборудования, если есть итог по ведомости:
     * отклонение; утверждение без штрафа; либо оплаченный штрафной счёт.
     */
    @Transactional
    public void tryAutoComplete(Long serviceRequestId) {
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId).orElse(null);
        if (sr == null) {
            return;
        }
        if (sr.getStatus() != ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW) {
            return;
        }
        if (sr.getReturnedAt() == null) {
            return;
        }
        Optional<DefectReport> reportOpt =
                defectReportRepository.findFirstByServiceRequest_IdOrderByCreatedAtDesc(serviceRequestId);
        if (reportOpt.isEmpty()) {
            return;
        }
        DefectReport report = reportOpt.get();
        if (report.getStatus() == DefectReportStatus.REJECTED) {
            markCompletedWithRentalWear(sr);
            return;
        }
        if (report.getStatus() != DefectReportStatus.APPROVED) {
            return;
        }
        BigDecimal penalty = report.getRecommendedPenalty();
        boolean needsPenaltyPayment =
                penalty != null && penalty.compareTo(BigDecimal.ZERO) > 0;
        if (!needsPenaltyPayment) {
            markCompletedWithRentalWear(sr);
            return;
        }
        Optional<Invoice> invoiceOpt = invoiceRepository.findTopByDefectReport_IdOrderByCreatedAtDesc(report.getId());
        if (invoiceOpt.isEmpty()) {
            return;
        }
        Invoice inv = invoiceOpt.get();
        if (inv.getInvoiceType() != InvoiceType.PENALTY) {
            return;
        }
        if (inv.getStatus() != InvoiceStatus.PAID) {
            return;
        }
        markCompletedWithRentalWear(sr);
    }

    private void markCompletedWithRentalWear(ServiceRequest sr) {
        sr.setStatus(ServiceRequestStatus.COMPLETED);
        serviceRequestRepository.save(sr);
        equipmentWearAccumulationService.applyRentalWearForCompletedRequest(sr.getId());
    }
}
