package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.AdminRequestStatusUpdate;
import com.example.rentalservice.api.dto.ServiceRequestResponse;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.InvoiceStatus;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.repository.InvoiceRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import com.example.rentalservice.repository.DefectReportRepository;
import java.util.Map;
import java.util.Set;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ManagerRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final DefectReportRepository defectReportRepository;
    private final ServiceRequestCompletionService serviceRequestCompletionService;

    public ManagerRequestService(
            ServiceRequestRepository serviceRequestRepository,
            InvoiceRepository invoiceRepository,
            DefectReportRepository defectReportRepository,
            ServiceRequestCompletionService serviceRequestCompletionService
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.invoiceRepository = invoiceRepository;
        this.defectReportRepository = defectReportRepository;
        this.serviceRequestCompletionService = serviceRequestCompletionService;
    }

    @Transactional
    public Page<ServiceRequestResponse> listRequests(ServiceRequestStatus status, Pageable pageable) {
        Specification<ServiceRequest> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        Page<ServiceRequest> page = serviceRequestRepository.findAll(spec, pageable);
        boolean dirty = false;
        for (ServiceRequest request : page.getContent()) {
            if (request.getReturnedAt() != null && request.getStatus() == ServiceRequestStatus.IN_PROGRESS) {
                request.setStatus(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW);
                dirty = true;
            }
        }
        if (dirty) {
            serviceRequestRepository.saveAll(page.getContent());
        }
        for (ServiceRequest request : page.getContent()) {
            serviceRequestCompletionService.tryAutoComplete(request.getId());
        }
        Page<ServiceRequest> refreshed = serviceRequestRepository.findAll(spec, pageable);
        return refreshed.map(this::toResponse);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(Long requestId, AdminRequestStatusUpdate payload) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getReturnedAt() != null && request.getStatus() == ServiceRequestStatus.IN_PROGRESS) {
            request.setStatus(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW);
        }
        ServiceRequestStatus current = request.getStatus();
        ServiceRequestStatus next = payload.status();
        if (current == ServiceRequestStatus.CANCELLED || current == ServiceRequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed request cannot be processed");
        }

        boolean transitionAllowed =
                (current == ServiceRequestStatus.NEW && (next == ServiceRequestStatus.AWAITING_PAYMENT || next == ServiceRequestStatus.CANCELLED))
                        || (current == ServiceRequestStatus.AWAITING_PAYMENT && (next == ServiceRequestStatus.IN_PROGRESS || next == ServiceRequestStatus.CANCELLED));
        if (!transitionAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
        }

        if (next == ServiceRequestStatus.IN_PROGRESS) {
            boolean isPaid = invoiceRepository.existsByServiceRequestIdAndStatus(requestId, InvoiceStatus.PAID);
            if (!isPaid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request can start only after invoice payment");
            }
        }
        request.setStatus(next);
        return toResponse(serviceRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statusSummary() {
        return Map.of(
                "new", serviceRequestRepository.countByStatus(ServiceRequestStatus.NEW),
                "awaitingPayment", serviceRequestRepository.countByStatus(ServiceRequestStatus.AWAITING_PAYMENT),
                "inProgress", serviceRequestRepository.countByStatus(ServiceRequestStatus.IN_PROGRESS),
                "awaitingSpecialistReview", serviceRequestRepository.countByStatus(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW),
                "completed", serviceRequestRepository.countByStatus(ServiceRequestStatus.COMPLETED),
                "cancelled", serviceRequestRepository.countByStatus(ServiceRequestStatus.CANCELLED)
        );
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> listInvoiceCandidates() {
        return serviceRequestRepository.findAll().stream()
                .filter(req -> req.getStatus() == ServiceRequestStatus.NEW
                        || req.getStatus() == ServiceRequestStatus.AWAITING_PAYMENT)
                .filter(req -> !invoiceRepository.existsByServiceRequestId(req.getId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteCancelledRequest(Long requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() != ServiceRequestStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only cancelled requests can be deleted");
        }
        if (invoiceRepository.existsByServiceRequestId(requestId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request has invoices and cannot be deleted");
        }
        serviceRequestRepository.delete(request);
    }

    private ServiceRequestResponse toResponse(ServiceRequest request) {
        int rentalDays = (int) (ChronoUnit.DAYS.between(request.getRentalStartDate(), request.getRentalEndDate()) + 1);
        boolean specialistConclusionRecorded = defectReportRepository.existsByServiceRequestIdAndStatusIn(
                request.getId(),
                Set.of(DefectReportStatus.SENT_TO_MANAGER, DefectReportStatus.APPROVED, DefectReportStatus.REJECTED)
        );
        return new ServiceRequestResponse(
                request.getId(),
                request.getService().getId(),
                request.getService().getTitle(),
                request.getEquipment().getId(),
                request.getEquipment().getInventoryCode(),
                request.getEquipment().getModelName(),
                request.getUser().getId(),
                request.getUser().getEmail(),
                request.getService().getBasePrice(),
                request.getRentalStartDate(),
                request.getRentalEndDate(),
                rentalDays,
                request.getReturnedAt(),
                request.getNotes(),
                request.getStatus(),
                request.getCreatedAt(),
                specialistConclusionRecorded
        );
    }
}
