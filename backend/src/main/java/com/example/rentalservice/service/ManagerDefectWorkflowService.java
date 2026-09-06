package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.DefectReportResponse;
import com.example.rentalservice.api.dto.ManagerDefectStatusUpdateRequest;
import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.Invoice;
import com.example.rentalservice.repository.DefectReportRepository;
import com.example.rentalservice.repository.InvoiceRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerDefectWorkflowService {

    private final DefectReportRepository defectReportRepository;
    private final BillingService billingService;
    private final InvoiceRepository invoiceRepository;
    private final ServiceRequestCompletionService serviceRequestCompletionService;
    private final EquipmentWearAccumulationService equipmentWearAccumulationService;

    public ManagerDefectWorkflowService(
            DefectReportRepository defectReportRepository,
            BillingService billingService,
            InvoiceRepository invoiceRepository,
            ServiceRequestCompletionService serviceRequestCompletionService,
            EquipmentWearAccumulationService equipmentWearAccumulationService
    ) {
        this.defectReportRepository = defectReportRepository;
        this.billingService = billingService;
        this.invoiceRepository = invoiceRepository;
        this.serviceRequestCompletionService = serviceRequestCompletionService;
        this.equipmentWearAccumulationService = equipmentWearAccumulationService;
    }

    @Transactional(readOnly = true)
    public List<DefectReportResponse> list(DefectReportStatus status) {
        List<DefectReport> reports = status == null
                ? defectReportRepository.findTop50ByOrderByCreatedAtDesc()
                : defectReportRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DefectReportResponse updateStatus(Long defectReportId, ManagerDefectStatusUpdateRequest payload) {
        DefectReport report = defectReportRepository.findById(defectReportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Defect report not found"));
        DefectReportStatus current = report.getStatus();
        DefectReportStatus next = payload.status();

        if (!isTransitionAllowed(current, next)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Illegal defect status transition: " + current + " -> " + next
            );
        }
        report.setStatus(next);
        DefectReport saved = defectReportRepository.save(report);
        if (next == DefectReportStatus.APPROVED) {
            equipmentWearAccumulationService.applyDefectWearForApprovedReport(saved.getId());
            billingService.issuePenaltyInvoiceForApprovedDefect(saved.getId());
        }
        if (saved.getServiceRequest() != null) {
            serviceRequestCompletionService.tryAutoComplete(saved.getServiceRequest().getId());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> summary() {
        return Map.of(
                "new", defectReportRepository.countByStatus(DefectReportStatus.NEW),
                "sentToManager", defectReportRepository.countByStatus(DefectReportStatus.SENT_TO_MANAGER),
                "approved", defectReportRepository.countByStatus(DefectReportStatus.APPROVED),
                "rejected", defectReportRepository.countByStatus(DefectReportStatus.REJECTED)
        );
    }

    private DefectReportResponse toResponse(DefectReport report) {
        Invoice penaltyInvoice = invoiceRepository.findTopByDefectReport_IdOrderByCreatedAtDesc(report.getId()).orElse(null);
        return new DefectReportResponse(
                report.getId(),
                report.getEquipment().getId(),
                report.getEquipment().getInventoryCode(),
                report.getEquipment().getModelName(),
                report.getSpecialist().getId(),
                report.getServiceRequest() == null ? null : report.getServiceRequest().getId(),
                penaltyInvoice == null ? null : penaltyInvoice.getId(),
                penaltyInvoice == null ? "NOT_ISSUED" : penaltyInvoice.getStatus().name(),
                report.getDefectDescription(),
                report.getSeverity(),
                report.getRecommendedPenalty(),
                report.getPlannedExtraWearPercent(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }

    private boolean isTransitionAllowed(DefectReportStatus current, DefectReportStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case NEW -> next == DefectReportStatus.SENT_TO_MANAGER || next == DefectReportStatus.REJECTED;
            case SENT_TO_MANAGER -> next == DefectReportStatus.APPROVED || next == DefectReportStatus.REJECTED;
            case APPROVED, REJECTED -> false;
        };
    }
}
