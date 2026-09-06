package com.example.rentalservice.api;

import com.example.rentalservice.api.dto.CreateDefectReportRequest;
import com.example.rentalservice.api.dto.UpdateSpecialistDefectReportRequest;
import com.example.rentalservice.api.dto.CreateNoDefectsConclusionRequest;
import com.example.rentalservice.api.dto.DefectReportResponse;
import com.example.rentalservice.api.dto.EquipmentOptionResponse;
import com.example.rentalservice.api.dto.EquipmentRepairAlertResponse;
import com.example.rentalservice.api.dto.EquipmentStateRecordedResponse;
import com.example.rentalservice.api.dto.EquipmentWearLedgerEntryResponse;
import com.example.rentalservice.api.dto.SpecialistEquipmentOverviewResponse;
import com.example.rentalservice.api.dto.SpecialistEquipmentTimelineEntryResponse;
import com.example.rentalservice.api.dto.RecordEquipmentStateRequest;
import com.example.rentalservice.api.dto.SpecialistServiceRequestOptionResponse;
import com.example.rentalservice.api.dto.SpecialistDefectStatusUpdateRequest;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.service.CurrentUserService;
import com.example.rentalservice.service.SpecialistService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/specialist")
@PreAuthorize("hasRole('SERVICE_SPECIALIST')")
public class SpecialistController {

    private final SpecialistService specialistService;
    private final CurrentUserService currentUserService;

    public SpecialistController(SpecialistService specialistService, CurrentUserService currentUserService) {
        this.specialistService = specialistService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/equipment/{equipmentId}/state")
    public EquipmentStateRecordedResponse recordState(
            @PathVariable Long equipmentId,
            @Valid @RequestBody RecordEquipmentStateRequest payload
    ) {
        return specialistService.recordState(equipmentId, payload);
    }

    @GetMapping("/equipment")
    public List<EquipmentOptionResponse> listEquipment() {
        return specialistService.listEquipmentOptions();
    }

    @GetMapping("/equipment/state-overview")
    public List<SpecialistEquipmentOverviewResponse> equipmentStateOverview() {
        return specialistService.listEquipmentStateOverview();
    }

    @GetMapping("/equipment/in-repair")
    public List<SpecialistEquipmentOverviewResponse> equipmentInRepair() {
        return specialistService.listEquipmentInRepair();
    }

    @GetMapping("/equipment/{equipmentId}/state-timeline")
    public List<SpecialistEquipmentTimelineEntryResponse> equipmentStateTimeline(@PathVariable Long equipmentId) {
        return specialistService.getEquipmentStateTimeline(equipmentId);
    }

    @GetMapping("/equipment/{equipmentId}/state")
    public List<EquipmentStateRecordedResponse> getRecentStates(@PathVariable Long equipmentId) {
        return specialistService.getRecentStates(equipmentId);
    }

    @GetMapping("/equipment/{equipmentId}/wear-ledger")
    public List<EquipmentWearLedgerEntryResponse> wearLedger(@PathVariable Long equipmentId) {
        return specialistService.listWearLedger(equipmentId);
    }

    @GetMapping("/equipment/repair-alerts")
    public List<EquipmentRepairAlertResponse> repairAlerts() {
        return specialistService.listRepairAlerts();
    }

    @PostMapping("/equipment/{equipmentId}/send-to-repair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendToRepair(@PathVariable Long equipmentId) {
        specialistService.sendToRepair(equipmentId);
    }

    @PostMapping("/equipment/{equipmentId}/complete-repair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeRepair(@PathVariable Long equipmentId) {
        specialistService.completeRepair(equipmentId);
    }

    @PostMapping("/defect-reports")
    public DefectReportResponse createDefectReport(
            @Valid @RequestBody CreateDefectReportRequest payload,
            Authentication authentication
    ) {
        return specialistService.createDefectReport(currentUserService.getCurrentUser(authentication), payload);
    }

    @GetMapping("/defect-reports")
    public List<DefectReportResponse> listDefectReports(
            @RequestParam(required = false) DefectReportStatus status,
            Authentication authentication
    ) {
        return specialistService.listDefectReports(currentUserService.getCurrentUser(authentication).getId(), status);
    }

    @GetMapping("/service-requests/active")
    public List<SpecialistServiceRequestOptionResponse> activeServiceRequests() {
        return specialistService.listActiveServiceRequests();
    }

    @GetMapping("/defect-reports/summary")
    public Map<String, Long> summary(Authentication authentication) {
        return specialistService.summary(currentUserService.getCurrentUser(authentication).getId());
    }

    @PatchMapping("/defect-reports/{defectReportId}")
    public DefectReportResponse updateDefectReport(
            @PathVariable Long defectReportId,
            @Valid @RequestBody UpdateSpecialistDefectReportRequest payload,
            Authentication authentication
    ) {
        return specialistService.updateDefectReport(currentUserService.getCurrentUser(authentication), defectReportId, payload);
    }

    @PatchMapping("/defect-reports/{defectReportId}/status")
    public DefectReportResponse updateDefectStatus(
            @PathVariable Long defectReportId,
            @Valid @RequestBody SpecialistDefectStatusUpdateRequest payload,
            Authentication authentication
    ) {
        return specialistService.updateStatus(currentUserService.getCurrentUser(authentication), defectReportId, payload);
    }

    @PostMapping("/defect-reports/no-defects")
    public DefectReportResponse createNoDefectsConclusion(
            @Valid @RequestBody CreateNoDefectsConclusionRequest payload,
            Authentication authentication
    ) {
        return specialistService.createNoDefectsConclusion(currentUserService.getCurrentUser(authentication), payload);
    }
}
