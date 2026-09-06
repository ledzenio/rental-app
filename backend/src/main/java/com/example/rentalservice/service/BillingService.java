package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.CreateInvoiceRequest;
import com.example.rentalservice.api.dto.InvoiceResponse;
import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.Invoice;
import com.example.rentalservice.domain.InvoiceStatus;
import com.example.rentalservice.domain.InvoiceType;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.repository.DefectReportRepository;
import com.example.rentalservice.repository.InvoiceRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import com.example.rentalservice.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BillingService {
    private static final Pattern LOGISTICS_PATTERN = Pattern.compile("Логистическая надбавка:\\s*([0-9]+(?:[.,][0-9]+)?)");

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final DefectReportRepository defectReportRepository;
    private final MailService mailService;
    private final ServiceRequestCompletionService serviceRequestCompletionService;

    public BillingService(
            InvoiceRepository invoiceRepository,
            UserRepository userRepository,
            ServiceRequestRepository serviceRequestRepository,
            DefectReportRepository defectReportRepository,
            MailService mailService,
            ServiceRequestCompletionService serviceRequestCompletionService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.defectReportRepository = defectReportRepository;
        this.mailService = mailService;
        this.serviceRequestCompletionService = serviceRequestCompletionService;
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest payload) {
        User user = userRepository.findById(payload.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ServiceRequest serviceRequest = null;
        if (payload.serviceRequestId() != null) {
            serviceRequest = serviceRequestRepository.findById(payload.serviceRequestId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));
        }

        DefectReport defectReport = null;
        if (payload.defectReportId() != null) {
            defectReport = defectReportRepository.findById(payload.defectReportId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Defect report not found"));
            if (payload.invoiceType() != InvoiceType.PENALTY) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Defect report can be billed only as PENALTY");
            }
            if (invoiceRepository.existsByDefectReport_Id(defectReport.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Счет по этой дефектной ведомости уже выставлен");
            }
        }
        if (payload.invoiceType() == InvoiceType.PENALTY && defectReport == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Penalty invoice requires defectReportId");
        }
        if (payload.invoiceType() == InvoiceType.PENALTY && payload.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Штрафной счёт с нулевой или отрицательной суммой не выставляется (нет оснований к оплате)."
            );
        }
        if (payload.invoiceType() == InvoiceType.SERVICE && serviceRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service invoice requires serviceRequestId");
        }
        if (payload.invoiceType() == InvoiceType.SERVICE && serviceRequest != null) {
            long rentalDays = ChronoUnit.DAYS.between(serviceRequest.getRentalStartDate(), serviceRequest.getRentalEndDate()) + 1;
            BigDecimal dailyBase = serviceRequest.getService().getBasePrice();
            BigDecimal rentalAmount = dailyBase.multiply(BigDecimal.valueOf(rentalDays));
            BigDecimal logisticsAmount = extractLogisticsSurcharge(serviceRequest.getNotes());
            BigDecimal minimumAmount = rentalAmount;
            BigDecimal recommendedAmount = rentalAmount.add(logisticsAmount);
            BigDecimal maximumAmount = recommendedAmount.multiply(BigDecimal.valueOf(1.25)).setScale(2, RoundingMode.HALF_UP);
            if (payload.amount().compareTo(minimumAmount) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Сумма сервисного счета меньше минимальной: " + minimumAmount + " BYN (" + rentalDays + " суток)."
                );
            }
            if (payload.amount().compareTo(maximumAmount) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Сумма сервисного счета превышает верхнюю границу: " + maximumAmount
                                + " BYN. Проверьте расчет по суткам и логистике."
                );
            }
        }
        if (serviceRequest != null && invoiceRepository.existsByServiceRequestId(serviceRequest.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice for this request has already been issued");
        }

        Invoice invoice = new Invoice();
        invoice.setUser(user);
        invoice.setServiceRequest(serviceRequest);
        invoice.setDefectReport(defectReport);
        invoice.setInvoiceType(payload.invoiceType());
        invoice.setAmount(payload.amount());
        invoice.setCurrency("BYN");
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setDescription(payload.description().trim());
        invoice.setCreatedAt(Instant.now());
        if (serviceRequest != null) {
            serviceRequest.setStatus(ServiceRequestStatus.AWAITING_PAYMENT);
        }
        Invoice saved = invoiceRepository.save(invoice);
        mailService.send(
                user.getEmail(),
                "Новый счет #" + saved.getId(),
                "Вам выставлен счет " + saved.getId() + " на сумму " + saved.getAmount() + " " + saved.getCurrency() + ". Зайдите в раздел 'Мои счета' для оплаты."
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getMyInvoices(User user) {
        return invoiceRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public InvoiceResponse payInvoice(User user, Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invoice belongs to another user");
        }
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is not payable");
        }
        if (user.getVirtualBalance().compareTo(invoice.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient virtual balance");
        }
        user.setVirtualBalance(user.getVirtualBalance().subtract(invoice.getAmount()));
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        if (invoice.getServiceRequest() != null && invoice.getServiceRequest().getStatus() == ServiceRequestStatus.AWAITING_PAYMENT) {
            ServiceRequest sr = invoice.getServiceRequest();
            // Уже отмечен возврат, но счёт доплатили позже — не возвращать в «в работе».
            if (sr.getReturnedAt() != null) {
                sr.setStatus(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW);
            } else {
                sr.setStatus(ServiceRequestStatus.IN_PROGRESS);
            }
        }
        userRepository.save(user);
        Invoice persisted = invoiceRepository.save(invoice);
        if (invoice.getInvoiceType() == InvoiceType.PENALTY && invoice.getDefectReport() != null) {
            DefectReport defectReport = invoice.getDefectReport();
            if (defectReport.getServiceRequest() != null) {
                serviceRequestCompletionService.tryAutoComplete(defectReport.getServiceRequest().getId());
            }
        }
        return toResponse(persisted);
    }

    /**
     * Выставляет клиенту штрафной счет по утвержденной дефектной ведомости (идемпотентно).
     */
    @Transactional
    public void issuePenaltyInvoiceForApprovedDefect(Long defectReportId) {
        DefectReport dr = defectReportRepository.findById(defectReportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Defect report not found"));
        if (dr.getStatus() != DefectReportStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Счет можно выставить только для утвержденной ведомости");
        }
        if (dr.getServiceRequest() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дефектная ведомость не привязана к заявке");
        }
        if (invoiceRepository.existsByDefectReport_Id(defectReportId)) {
            return;
        }
        User client = dr.getServiceRequest().getUser();
        BigDecimal amount = dr.getRecommendedPenalty();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            // В т.ч. заключение «дефекты не выявлены»: счёт и уведомление клиенту не нужны.
            return;
        }
        Long requestId = dr.getServiceRequest().getId();
        String serviceTitle = dr.getServiceRequest().getService().getTitle();
        String description = "Штраф по дефектной ведомости #" + dr.getId()
                + " (заявка #" + requestId + ", " + serviceTitle + ")";
        createInvoice(new CreateInvoiceRequest(
                client.getId(),
                null,
                defectReportId,
                InvoiceType.PENALTY,
                amount,
                description
        ));
    }

    private BigDecimal extractLogisticsSurcharge(String notes) {
        if (notes == null || notes.isBlank()) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = LOGISTICS_PATTERN.matcher(notes);
        if (!matcher.find()) {
            return BigDecimal.ZERO;
        }
        String raw = matcher.group(1).replace(',', '.');
        try {
            return new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        String serviceTitle = null;
        LocalDate rentalStartDate = null;
        LocalDate rentalEndDate = null;
        ServiceRequest sr = invoice.getServiceRequest();
        if (sr != null) {
            if (sr.getService() != null) {
                serviceTitle = sr.getService().getTitle();
            }
            rentalStartDate = sr.getRentalStartDate();
            rentalEndDate = sr.getRentalEndDate();
        }
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getUser().getId(),
                sr == null ? null : sr.getId(),
                invoice.getDefectReport() == null ? null : invoice.getDefectReport().getId(),
                invoice.getInvoiceType(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getDescription(),
                invoice.getCreatedAt(),
                invoice.getPaidAt(),
                serviceTitle,
                rentalStartDate,
                rentalEndDate
        );
    }
}
