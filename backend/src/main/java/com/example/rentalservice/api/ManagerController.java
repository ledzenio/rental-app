package com.example.rentalservice.api;

import com.example.rentalservice.api.dto.AdminRequestStatusUpdate;
import com.example.rentalservice.api.dto.AdminCreateUserRequest;
import com.example.rentalservice.api.dto.AdminUpdateUserRequest;
import com.example.rentalservice.api.dto.AdminUserResponse;
import com.example.rentalservice.api.dto.CreateInvoiceRequest;
import com.example.rentalservice.api.dto.DefectReportResponse;
import com.example.rentalservice.api.dto.InvoiceResponse;
import com.example.rentalservice.api.dto.ManagerDefectStatusUpdateRequest;
import com.example.rentalservice.api.dto.ManagerServiceCatalogUpsertRequest;
import com.example.rentalservice.api.dto.RevenueReportResponse;
import com.example.rentalservice.api.dto.AnalyticsReportResponse;
import com.example.rentalservice.api.dto.ServiceCatalogItemResponse;
import com.example.rentalservice.api.dto.ServiceRequestResponse;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.service.ManagerRequestService;
import com.example.rentalservice.service.ManagerServiceCatalogService;
import com.example.rentalservice.service.ManagerUserService;
import com.example.rentalservice.service.BillingService;
import com.example.rentalservice.service.ManagerDefectWorkflowService;
import com.example.rentalservice.service.RevenueReportService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final ManagerUserService managerUserService;
    private final ManagerRequestService managerRequestService;
    private final ManagerServiceCatalogService managerServiceCatalogService;
    private final BillingService billingService;
    private final ManagerDefectWorkflowService managerDefectWorkflowService;
    private final RevenueReportService revenueReportService;

    public ManagerController(
            ManagerUserService managerUserService,
            ManagerRequestService managerRequestService,
            ManagerServiceCatalogService managerServiceCatalogService,
            BillingService billingService,
            ManagerDefectWorkflowService managerDefectWorkflowService,
            RevenueReportService revenueReportService
    ) {
        this.managerUserService = managerUserService;
        this.managerRequestService = managerRequestService;
        this.managerServiceCatalogService = managerServiceCatalogService;
        this.billingService = billingService;
        this.managerDefectWorkflowService = managerDefectWorkflowService;
        this.revenueReportService = revenueReportService;
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return managerUserService.listUsers(query, pageable);
    }

    @GetMapping("/users/{userId}")
    public AdminUserResponse getUser(@PathVariable Long userId) {
        return managerUserService.getUser(userId);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createUser(@Valid @RequestBody AdminCreateUserRequest payload) {
        return managerUserService.createUser(payload);
    }

    @PatchMapping("/users/{userId}")
    public AdminUserResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest payload
    ) {
        return managerUserService.updateUser(userId, payload);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        managerUserService.deleteUser(userId);
    }

    @GetMapping("/service-requests")
    public Page<ServiceRequestResponse> listRequests(
            @RequestParam(required = false) ServiceRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return managerRequestService.listRequests(status, pageable);
    }

    @PatchMapping("/service-requests/{requestId}/status")
    public ServiceRequestResponse updateRequestStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody AdminRequestStatusUpdate payload
    ) {
        return managerRequestService.updateStatus(requestId, payload);
    }

    @DeleteMapping("/service-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCancelledRequest(@PathVariable Long requestId) {
        managerRequestService.deleteCancelledRequest(requestId);
    }

    @GetMapping("/service-requests/summary")
    public Map<String, Long> summary() {
        return managerRequestService.statusSummary();
    }

    @GetMapping("/service-requests/invoice-candidates")
    public List<ServiceRequestResponse> invoiceCandidates() {
        return managerRequestService.listInvoiceCandidates();
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceCatalogItemResponse createService(@Valid @RequestBody ManagerServiceCatalogUpsertRequest payload) {
        return managerServiceCatalogService.create(payload);
    }

    @PutMapping("/services/{serviceId}")
    public ServiceCatalogItemResponse updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody ManagerServiceCatalogUpsertRequest payload
    ) {
        return managerServiceCatalogService.update(serviceId, payload);
    }

    @PatchMapping("/services/{serviceId}/activate")
    public ServiceCatalogItemResponse activateService(@PathVariable Long serviceId) {
        return managerServiceCatalogService.setActive(serviceId, true);
    }

    @DeleteMapping("/services/{serviceId}")
    public ServiceCatalogItemResponse deactivateService(@PathVariable Long serviceId) {
        return managerServiceCatalogService.setActive(serviceId, false);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest payload) {
        return billingService.createInvoice(payload);
    }

    @GetMapping("/defect-reports")
    public List<DefectReportResponse> listDefectReports(@RequestParam(required = false) DefectReportStatus status) {
        return managerDefectWorkflowService.list(status);
    }

    @PatchMapping("/defect-reports/{defectReportId}/status")
    public DefectReportResponse updateDefectStatus(
            @PathVariable Long defectReportId,
            @Valid @RequestBody ManagerDefectStatusUpdateRequest payload
    ) {
        return managerDefectWorkflowService.updateStatus(defectReportId, payload);
    }

    @GetMapping("/defect-reports/summary")
    public Map<String, Long> defectSummary() {
        return managerDefectWorkflowService.summary();
    }

    @GetMapping("/reports/revenue")
    public RevenueReportResponse revenueReport(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {
        ensureReportDateRange(fromDate, toDate);
        return revenueReportService.build(fromDate, toDate);
    }

    @GetMapping("/reports/analytics")
    public AnalyticsReportResponse analyticsReport(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {
        ensureReportDateRange(fromDate, toDate);
        return revenueReportService.buildAnalytics(fromDate, toDate);
    }

    @GetMapping(value = "/reports/revenue/export", produces = "text/csv")
    public String exportRevenueReportCsv(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {
        ensureReportDateRange(fromDate, toDate);
        return revenueReportService.buildCsv(fromDate, toDate);
    }

    private void ensureReportDateRange(LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toDate не может быть раньше fromDate.");
        }
    }
}
