package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.CreateDefectReportRequest;
import com.example.rentalservice.api.dto.CreateNoDefectsConclusionRequest;
import com.example.rentalservice.api.dto.DefectReportResponse;
import com.example.rentalservice.api.dto.EquipmentOptionResponse;
import com.example.rentalservice.api.dto.EquipmentRepairAlertResponse;
import com.example.rentalservice.api.dto.EquipmentStateRecordedResponse;
import com.example.rentalservice.api.dto.EquipmentWearLedgerEntryResponse;
import com.example.rentalservice.api.dto.RecordEquipmentStateRequest;
import com.example.rentalservice.api.dto.SpecialistEquipmentInspectionSnapshotResponse;
import com.example.rentalservice.api.dto.SpecialistEquipmentOverviewResponse;
import com.example.rentalservice.api.dto.SpecialistEquipmentStateEventType;
import com.example.rentalservice.api.dto.SpecialistEquipmentTimelineEntryResponse;
import com.example.rentalservice.api.dto.SpecialistServiceRequestOptionResponse;
import com.example.rentalservice.api.dto.UpdateSpecialistDefectReportRequest;
import com.example.rentalservice.api.dto.SpecialistDefectStatusUpdateRequest;
import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectSeverity;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.Equipment;
import com.example.rentalservice.domain.EquipmentLifecycleStatus;
import com.example.rentalservice.domain.EquipmentStateHistory;
import com.example.rentalservice.domain.EquipmentWearLedger;
import com.example.rentalservice.domain.ServiceCatalogItem;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.domain.WearLedgerEntryType;
import com.example.rentalservice.repository.DefectReportRepository;
import com.example.rentalservice.repository.EquipmentRepository;
import com.example.rentalservice.repository.EquipmentStateHistoryRepository;
import com.example.rentalservice.repository.EquipmentWearLedgerRepository;
import com.example.rentalservice.repository.InvoiceRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import com.example.rentalservice.service.wear.DefectWearBonusPolicy;
import com.example.rentalservice.service.wear.WearCalculationInput;
import com.example.rentalservice.service.wear.WearCalculationResult;
import com.example.rentalservice.service.wear.WearCalculationStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SpecialistService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentStateHistoryRepository equipmentStateHistoryRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final DefectReportRepository defectReportRepository;
    private final InvoiceRepository invoiceRepository;
    private final EquipmentWearLedgerRepository equipmentWearLedgerRepository;
    private final WearCalculationStrategy wearCalculationStrategy;

    public SpecialistService(
            EquipmentRepository equipmentRepository,
            EquipmentStateHistoryRepository equipmentStateHistoryRepository,
            ServiceRequestRepository serviceRequestRepository,
            DefectReportRepository defectReportRepository,
            InvoiceRepository invoiceRepository,
            EquipmentWearLedgerRepository equipmentWearLedgerRepository,
            WearCalculationStrategy wearCalculationStrategy
    ) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentStateHistoryRepository = equipmentStateHistoryRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.defectReportRepository = defectReportRepository;
        this.invoiceRepository = invoiceRepository;
        this.equipmentWearLedgerRepository = equipmentWearLedgerRepository;
        this.wearCalculationStrategy = wearCalculationStrategy;
    }

    @Transactional
    public EquipmentStateRecordedResponse recordState(Long equipmentId, RecordEquipmentStateRequest payload) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        boolean atClient = serviceRequestRepository.existsByEquipmentIdAndStatusInAndReturnedAtIsNull(
                equipmentId,
                Set.of(ServiceRequestStatus.IN_PROGRESS)
        );
        if (atClient) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя фиксировать состояние: оборудование сейчас у клиента.");
        }

        WearCalculationResult result = wearCalculationStrategy.calculate(new WearCalculationInput(
                equipment.getBaseWearPercent(),
                equipment.getPurchasePrice(),
                payload.conditionLabel(),
                payload.batteryPercent(),
                payload.temperatureCelsius(),
                payload.operatingHoursDelta()
        ));

        EquipmentStateHistory history = new EquipmentStateHistory();
        history.setEquipment(equipment);
        history.setConditionLabel(payload.conditionLabel().trim());
        history.setBatteryPercent(payload.batteryPercent());
        history.setTemperatureCelsius(payload.temperatureCelsius());
        history.setCalculatedWearPercent(result.calculatedWearPercent());
        history.setAmortizationValue(result.amortizationValue());
        history.setNotes(payload.notes());
        history.setRecordedAt(Instant.now());
        EquipmentStateHistory saved = equipmentStateHistoryRepository.save(history);
        boolean critical = isCriticalState(payload.conditionLabel(), result.calculatedWearPercent());
        if (critical && equipment.getLifecycleStatus() != EquipmentLifecycleStatus.UNDER_REPAIR) {
            equipment.setLifecycleStatus(EquipmentLifecycleStatus.NEEDS_REPAIR);
            equipmentRepository.save(equipment);
        }

        return new EquipmentStateRecordedResponse(
                saved.getId(),
                equipment.getId(),
                saved.getConditionLabel(),
                saved.getCalculatedWearPercent(),
                saved.getAmortizationValue(),
                saved.getRecordedAt(),
                critical
        );
    }

    @Transactional(readOnly = true)
    public List<EquipmentWearLedgerEntryResponse> listWearLedger(Long equipmentId) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found");
        }
        return equipmentWearLedgerRepository.findTop50ByEquipment_IdOrderByCreatedAtDesc(equipmentId).stream()
                .map(row -> new EquipmentWearLedgerEntryResponse(
                        row.getId(),
                        row.getEntryType(),
                        row.getServiceRequest() == null ? null : row.getServiceRequest().getId(),
                        row.getDefectReport() == null ? null : row.getDefectReport().getId(),
                        row.getRentalDays(),
                        row.getRentalRatePerDay(),
                        row.getRentalWearDelta(),
                        row.getDefectWearDelta(),
                        row.getTotalWearDelta(),
                        row.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentStateRecordedResponse> getRecentStates(Long equipmentId) {
        return equipmentStateHistoryRepository.findTop20ByEquipmentIdOrderByRecordedAtDesc(equipmentId).stream()
                .map(h -> new EquipmentStateRecordedResponse(
                        h.getId(),
                        h.getEquipment().getId(),
                        h.getConditionLabel(),
                        h.getCalculatedWearPercent(),
                        h.getAmortizationValue(),
                        h.getRecordedAt(),
                        isCriticalState(h.getConditionLabel(), h.getCalculatedWearPercent())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentOptionResponse> listEquipmentOptions() {
        Set<Long> equipmentAtClient = Set.copyOf(serviceRequestRepository.findDistinctEquipmentIdsByStatusInAndReturnedAtIsNull(
                Set.of(ServiceRequestStatus.IN_PROGRESS)
        ));
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getLifecycleStatus() != EquipmentLifecycleStatus.UNDER_REPAIR)
                .filter(e -> !equipmentAtClient.contains(e.getId()))
                .map(e -> new EquipmentOptionResponse(
                        e.getId(),
                        e.getInventoryCode(),
                        e.getModelName(),
                        e.getAccumulatedWearPercent(),
                        e.getRentalWearRatePerDay()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialistEquipmentOverviewResponse> listEquipmentStateOverview() {
        Set<Long> equipmentAtClient = Set.copyOf(serviceRequestRepository.findDistinctEquipmentIdsByStatusInAndReturnedAtIsNull(
                Set.of(ServiceRequestStatus.IN_PROGRESS)
        ));
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getLifecycleStatus() != EquipmentLifecycleStatus.UNDER_REPAIR)
                .filter(e -> !equipmentAtClient.contains(e.getId()))
                .map(this::toEquipmentOverview)
                .sorted(Comparator
                        .comparing(SpecialistEquipmentOverviewResponse::category, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(SpecialistEquipmentOverviewResponse::inventoryCode, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialistEquipmentOverviewResponse> listEquipmentInRepair() {
        Set<Long> equipmentAtClient = Set.copyOf(serviceRequestRepository.findDistinctEquipmentIdsByStatusInAndReturnedAtIsNull(
                Set.of(ServiceRequestStatus.IN_PROGRESS)
        ));
        return equipmentRepository.findAllByLifecycleStatusOrderByIdAsc(EquipmentLifecycleStatus.UNDER_REPAIR).stream()
                .filter(e -> !equipmentAtClient.contains(e.getId()))
                .map(this::toEquipmentOverview)
                .sorted(Comparator
                        .comparing(SpecialistEquipmentOverviewResponse::category, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(SpecialistEquipmentOverviewResponse::inventoryCode, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialistEquipmentTimelineEntryResponse> getEquipmentStateTimeline(Long equipmentId) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found");
        }
        List<SpecialistEquipmentTimelineEntryResponse> entries = new ArrayList<>();
        for (EquipmentStateHistory h : equipmentStateHistoryRepository.findTop100ByEquipmentIdOrderByRecordedAtDesc(equipmentId)) {
            entries.add(toInspectionTimelineEntry(h));
        }
        for (EquipmentWearLedger w : equipmentWearLedgerRepository.findTop50ByEquipment_IdOrderByCreatedAtDesc(equipmentId)) {
            if (w.getEntryType() == WearLedgerEntryType.RENTAL_CLOSURE) {
                entries.add(toRentalWearTimelineEntry(w));
            } else if (w.getEntryType() == WearLedgerEntryType.DEFECT_APPROVAL) {
                entries.add(toDefectWearTimelineEntry(w));
            }
        }
        entries.sort(Comparator.comparing(SpecialistEquipmentTimelineEntryResponse::occurredAt).reversed());
        return entries;
    }

    private SpecialistEquipmentOverviewResponse toEquipmentOverview(Equipment e) {
        var latestOpt = equipmentStateHistoryRepository.findTopByEquipmentIdOrderByRecordedAtDesc(e.getId());
        ServiceCatalogItem cat = e.getServiceCatalogItem();
        String category = (cat != null && cat.getCategory() != null && !cat.getCategory().isBlank())
                ? cat.getCategory()
                : "Прочее";
        String catalogTitle = cat != null ? cat.getTitle() : "—";
        SpecialistEquipmentInspectionSnapshotResponse snap = latestOpt
                .map(latest -> new SpecialistEquipmentInspectionSnapshotResponse(
                        latest.getId(),
                        latest.getConditionLabel(),
                        latest.getCalculatedWearPercent(),
                        latest.getAmortizationValue(),
                        latest.getRecordedAt()
                ))
                .orElse(null);
        BigDecimal acc = e.getAccumulatedWearPercent() != null ? e.getAccumulatedWearPercent() : BigDecimal.ZERO;
        BigDecimal rate = e.getRentalWearRatePerDay() != null ? e.getRentalWearRatePerDay() : new BigDecimal("0.0800");
        return new SpecialistEquipmentOverviewResponse(
                e.getId(),
                e.getInventoryCode(),
                e.getModelName(),
                category,
                catalogTitle,
                e.getLifecycleStatus(),
                acc,
                rate,
                snap
        );
    }

    private SpecialistEquipmentTimelineEntryResponse toInspectionTimelineEntry(EquipmentStateHistory h) {
        boolean postRepair = isPostRepairCondition(h.getConditionLabel());
        String title = postRepair ? "Возврат из ремонта" : "Осмотр оборудования";
        StringBuilder desc = new StringBuilder();
        if (postRepair) {
            desc.append("Сервисный ремонт завершён; зафиксировано обновлённое состояние («").append(h.getConditionLabel()).append("»).");
        } else {
            desc.append("Осмотр специалиста: внешнее состояние «").append(h.getConditionLabel()).append("».");
        }
        if (h.getBatteryPercent() != null) {
            desc.append("\nЗаряд аккумулятора: ").append(h.getBatteryPercent()).append("%.");
        }
        if (h.getTemperatureCelsius() != null) {
            desc.append("\nТемпература: ").append(h.getTemperatureCelsius().toPlainString()).append(" °C.");
        }
        desc.append("\nРасчётный износ по методике осмотра: ").append(h.getCalculatedWearPercent().toPlainString()).append("%.");
        desc.append("\nОценка амортизации: ").append(h.getAmortizationValue().toPlainString()).append(" BYN.");
        if (h.getNotes() != null && !h.getNotes().isBlank()) {
            desc.append("\nКомментарий: ").append(h.getNotes().trim());
        }
        return new SpecialistEquipmentTimelineEntryResponse(
                SpecialistEquipmentStateEventType.INSPECTION,
                h.getRecordedAt(),
                title,
                desc.toString(),
                h.getId(),
                null,
                null,
                null,
                h.getCalculatedWearPercent(),
                null,
                null,
                null,
                null
        );
    }

    private SpecialistEquipmentTimelineEntryResponse toRentalWearTimelineEntry(EquipmentWearLedger w) {
        Long srId = w.getServiceRequest() == null ? null : w.getServiceRequest().getId();
        StringBuilder desc = new StringBuilder();
        desc.append("Износ накоплен по завершении аренды (календарные сутки и индивидуальная норма на единицу).");
        if (w.getRentalDays() != null) {
            desc.append("\nСуток в расчёте: ").append(w.getRentalDays()).append(".");
        }
        if (w.getRentalRatePerDay() != null) {
            desc.append("\nНорма на сутки: ").append(w.getRentalRatePerDay().toPlainString()).append("%.");
        }
        if (w.getRentalWearDelta() != null) {
            desc.append("\nПрирост износа за период: +").append(w.getRentalWearDelta().toPlainString()).append("%.");
        }
        if (srId != null) {
            desc.append("\nСвязанная заявка: №").append(srId).append(".");
        }
        return new SpecialistEquipmentTimelineEntryResponse(
                SpecialistEquipmentStateEventType.RENTAL_WEAR,
                w.getCreatedAt(),
                "Износ использования (аренда)",
                desc.toString(),
                null,
                w.getId(),
                srId,
                null,
                null,
                w.getRentalWearDelta(),
                null,
                w.getRentalDays(),
                w.getRentalRatePerDay()
        );
    }

    private SpecialistEquipmentTimelineEntryResponse toDefectWearTimelineEntry(EquipmentWearLedger w) {
        Long drId = w.getDefectReport() == null ? null : w.getDefectReport().getId();
        Long srId = w.getServiceRequest() == null ? null : w.getServiceRequest().getId();
        StringBuilder desc = new StringBuilder();
        desc.append("После утверждения дефектной ведомости к накопленному износу добавлена надбавка за состояние при приёме.");
        if (w.getDefectWearDelta() != null) {
            desc.append("\nПрирост износа: +").append(w.getDefectWearDelta().toPlainString()).append("%.");
        }
        if (drId != null) {
            desc.append("\nДефектная ведомость: №").append(drId).append(".");
        }
        if (srId != null) {
            desc.append("\nЗаявка: №").append(srId).append(".");
        }
        return new SpecialistEquipmentTimelineEntryResponse(
                SpecialistEquipmentStateEventType.DEFECT_WEAR,
                w.getCreatedAt(),
                "Износ по дефектной ведомости",
                desc.toString(),
                null,
                w.getId(),
                srId,
                drId,
                null,
                null,
                w.getDefectWearDelta(),
                null,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<EquipmentRepairAlertResponse> listRepairAlerts() {
        return equipmentRepository.findAllByLifecycleStatusOrderByIdAsc(EquipmentLifecycleStatus.NEEDS_REPAIR).stream()
                .filter(e -> !serviceRequestRepository.existsByEquipmentIdAndStatusInAndReturnedAtIsNull(
                        e.getId(),
                        Set.of(ServiceRequestStatus.IN_PROGRESS)
                ))
                .map(e -> {
                    EquipmentStateHistory latest = equipmentStateHistoryRepository
                            .findTopByEquipmentIdOrderByRecordedAtDesc(e.getId())
                            .orElse(null);
                    return new EquipmentRepairAlertResponse(
                            e.getId(),
                            e.getInventoryCode(),
                            e.getModelName(),
                            e.getAccumulatedWearPercent(),
                            latest == null ? null : latest.getId(),
                            latest == null ? null : latest.getConditionLabel(),
                            latest == null ? null : latest.getCalculatedWearPercent(),
                            latest == null ? null : latest.getAmortizationValue(),
                            latest == null ? null : latest.getRecordedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void sendToRepair(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        boolean atClient = serviceRequestRepository.existsByEquipmentIdAndStatusInAndReturnedAtIsNull(
                equipmentId,
                Set.of(ServiceRequestStatus.IN_PROGRESS)
        );
        if (atClient) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя отправить в ремонт: оборудование у клиента.");
        }
        if (equipment.getLifecycleStatus() != EquipmentLifecycleStatus.NEEDS_REPAIR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Оборудование не помечено как требующее ремонта.");
        }
        equipment.setLifecycleStatus(EquipmentLifecycleStatus.UNDER_REPAIR);
        equipmentRepository.save(equipment);
    }

    @Transactional
    public void completeRepair(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        if (equipment.getLifecycleStatus() != EquipmentLifecycleStatus.UNDER_REPAIR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Оборудование не находится в ремонте.");
        }
        boolean atClient = serviceRequestRepository.existsByEquipmentIdAndStatusInAndReturnedAtIsNull(
                equipmentId,
                Set.of(ServiceRequestStatus.IN_PROGRESS)
        );
        if (atClient) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя завершить ремонт: оборудование сейчас у клиента.");
        }
        equipment.setLifecycleStatus(EquipmentLifecycleStatus.AVAILABLE);
        equipmentRepository.save(equipment);

        String postRepairNotes =
                "Возврат из ремонта: характеристики обновлены, узлы и системы приведены в рабочее состояние по акту сервиса.";
        WearCalculationResult result = wearCalculationStrategy.calculate(new WearCalculationInput(
                equipment.getBaseWearPercent(),
                equipment.getPurchasePrice(),
                "ПОСЛЕ РЕМОНТА",
                null,
                null,
                null
        ));
        EquipmentStateHistory history = new EquipmentStateHistory();
        history.setEquipment(equipment);
        history.setConditionLabel("ПОСЛЕ РЕМОНТА");
        history.setBatteryPercent(null);
        history.setTemperatureCelsius(null);
        history.setCalculatedWearPercent(result.calculatedWearPercent());
        history.setAmortizationValue(result.amortizationValue());
        history.setNotes(postRepairNotes);
        history.setRecordedAt(Instant.now());
        equipmentStateHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<SpecialistServiceRequestOptionResponse> listActiveServiceRequests() {
        return serviceRequestRepository.findAllByStatusInOrderByCreatedAtDesc(Set.of(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW)).stream()
                .filter(r -> r.getReturnedAt() != null)
                .map(r -> new SpecialistServiceRequestOptionResponse(
                        r.getId(),
                        r.getService().getId(),
                        r.getService().getTitle(),
                        r.getUser().getEmail(),
                        r.getEquipment().getId(),
                        r.getEquipment().getInventoryCode(),
                        r.getEquipment().getModelName(),
                        equipmentStateHistoryRepository.findTopByEquipmentIdOrderByRecordedAtDesc(r.getEquipment().getId())
                                .map(h -> new SpecialistServiceRequestOptionResponse.EquipmentSnapshot(
                                        h.getId(),
                                        h.getConditionLabel(),
                                        h.getCalculatedWearPercent(),
                                        h.getAmortizationValue(),
                                        h.getRecordedAt()
                                ))
                                .orElse(null)
                ))
                .toList();
    }

    @Transactional
    public DefectReportResponse createDefectReport(User specialist, CreateDefectReportRequest payload) {
        if (payload.serviceRequestId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для ведомости необходимо выбрать заявку.");
        }
        ServiceRequest linkedRequest = serviceRequestRepository.findById(payload.serviceRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));
        if (linkedRequest.getStatus() != ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ведомость создается только для заявки в ожидании заключения специалиста.");
        }
        if (linkedRequest.getReturnedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ведомость можно составить только после возврата оборудования клиентом.");
        }
        if (payload.equipmentId() != null && !linkedRequest.getEquipment().getId().equals(payload.equipmentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Оборудование должно соответствовать выбранной заявке.");
        }
        Equipment equipment = linkedRequest.getEquipment();

        DefectReport report = new DefectReport();
        report.setEquipment(equipment);
        report.setSpecialist(specialist);
        report.setServiceRequest(linkedRequest);
        report.setDefectDescription(payload.defectDescription().trim());
        report.setSeverity(payload.severity());
        report.setPlannedExtraWearPercent(DefectWearBonusPolicy.plannedExtraWearForSeverity(payload.severity()));
        report.setRecommendedPenalty(payload.recommendedPenalty());
        report.setStatus(DefectReportStatus.NEW);
        report.setCreatedAt(Instant.now());
        return toResponse(defectReportRepository.save(report));
    }

    @Transactional
    public DefectReportResponse createNoDefectsConclusion(User specialist, CreateNoDefectsConclusionRequest payload) {
        ServiceRequest linkedRequest = serviceRequestRepository.findById(payload.serviceRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));
        if (linkedRequest.getStatus() != ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заключение можно отправить только по заявке в ожидании заключения специалиста.");
        }
        if (linkedRequest.getReturnedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заключение можно отправить только после возврата оборудования клиентом.");
        }
        if (payload.equipmentId() != null && !linkedRequest.getEquipment().getId().equals(payload.equipmentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Оборудование должно соответствовать выбранной заявке.");
        }
        boolean alreadyExistsForRequest = defectReportRepository.existsByServiceRequestIdAndStatusIn(
                linkedRequest.getId(),
                Set.of(DefectReportStatus.NEW, DefectReportStatus.SENT_TO_MANAGER, DefectReportStatus.APPROVED, DefectReportStatus.REJECTED)
        );
        if (alreadyExistsForRequest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "По данной заявке уже есть заключение специалиста.");
        }
        Equipment equipment = linkedRequest.getEquipment();
        DefectReport report = new DefectReport();
        report.setEquipment(equipment);
        report.setSpecialist(specialist);
        report.setServiceRequest(linkedRequest);
        report.setDefectDescription("Дефекты не выявлены. " + payload.conclusionText().trim());
        report.setSeverity(DefectSeverity.LOW);
        report.setPlannedExtraWearPercent(BigDecimal.ZERO);
        report.setRecommendedPenalty(BigDecimal.ZERO);
        report.setStatus(DefectReportStatus.SENT_TO_MANAGER);
        report.setCreatedAt(Instant.now());
        return toResponse(defectReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<DefectReportResponse> listDefectReports(Long specialistUserId, DefectReportStatus status) {
        List<DefectReport> reports = status == null
                ? defectReportRepository.findTop50BySpecialistIdOrderByCreatedAtDesc(specialistUserId)
                : defectReportRepository.findAllBySpecialistIdAndStatusOrderByCreatedAtDesc(specialistUserId, status);
        return reports.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> summary(Long specialistUserId) {
        return Map.of(
                "new", defectReportRepository.countBySpecialistIdAndStatus(specialistUserId, DefectReportStatus.NEW),
                "sentToManager", defectReportRepository.countBySpecialistIdAndStatus(specialistUserId, DefectReportStatus.SENT_TO_MANAGER),
                "approved", defectReportRepository.countBySpecialistIdAndStatus(specialistUserId, DefectReportStatus.APPROVED),
                "rejected", defectReportRepository.countBySpecialistIdAndStatus(specialistUserId, DefectReportStatus.REJECTED)
        );
    }

    @Transactional
    public DefectReportResponse updateDefectReport(User specialist, Long defectReportId, UpdateSpecialistDefectReportRequest payload) {
        DefectReport report = defectReportRepository.findById(defectReportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Defect report not found"));
        if (!report.getSpecialist().getId().equals(specialist.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно изменять только свои дефектные ведомости.");
        }
        if (report.getStatus() == DefectReportStatus.APPROVED || report.getStatus() == DefectReportStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ведомость уже рассмотрена менеджером, правки недоступны.");
        }
        String trimmed = payload.defectDescription().trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Описание не может быть пустым.");
        }
        if (isNoDefectsConclusionReport(report)) {
            report.setDefectDescription(trimmed.startsWith("Дефекты не выявлены")
                    ? trimmed
                    : "Дефекты не выявлены. " + trimmed);
            return toResponse(defectReportRepository.save(report));
        }
        report.setDefectDescription(trimmed);
        report.setSeverity(payload.severity());
        report.setRecommendedPenalty(payload.recommendedPenalty());
        report.setPlannedExtraWearPercent(DefectWearBonusPolicy.plannedExtraWearForSeverity(payload.severity()));
        return toResponse(defectReportRepository.save(report));
    }

    private boolean isNoDefectsConclusionReport(DefectReport report) {
        String d = report.getDefectDescription();
        return d != null && d.trim().startsWith("Дефекты не выявлены");
    }

    @Transactional
    public DefectReportResponse updateStatus(User specialist, Long defectReportId, SpecialistDefectStatusUpdateRequest payload) {
        DefectReport report = defectReportRepository.findById(defectReportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Defect report not found"));
        if (!report.getSpecialist().getId().equals(specialist.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Можно изменять только свои дефектные ведомости.");
        }
        if (report.getStatus() != DefectReportStatus.NEW || payload.status() != DefectReportStatus.SENT_TO_MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Специалист может только передать новую ведомость менеджеру (NEW -> SENT_TO_MANAGER)."
            );
        }
        report.setStatus(DefectReportStatus.SENT_TO_MANAGER);
        return toResponse(defectReportRepository.save(report));
    }

    private DefectReportResponse toResponse(DefectReport report) {
        var penaltyInvoice = invoiceRepository.findTopByDefectReport_IdOrderByCreatedAtDesc(report.getId()).orElse(null);
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

    private static boolean isPostRepairCondition(String conditionLabel) {
        if (conditionLabel == null || conditionLabel.isBlank()) {
            return false;
        }
        String n = conditionLabel.trim().toUpperCase(Locale.ROOT);
        return "ПОСЛЕ РЕМОНТА".equals(n) || "POST_REPAIR".equals(n);
    }

    /**
     * Аварийная / критическая оценка внешнего состояния при любом осмотре (recordState) — рекомендация ремонта (NEEDS_REPAIR).
     * Дополнительно: расчётный износ по методике осмотра ≥ 80%.
     */
    private boolean isCriticalState(String conditionLabel, java.math.BigDecimal wearPercent) {
        boolean criticalByLabel = isEmergencyConditionLabel(conditionLabel);
        boolean criticalByWear = wearPercent != null && wearPercent.compareTo(java.math.BigDecimal.valueOf(80)) >= 0;
        return criticalByLabel || criticalByWear;
    }

    private static boolean isEmergencyConditionLabel(String conditionLabel) {
        if (conditionLabel == null || conditionLabel.isBlank()) {
            return false;
        }
        String n = conditionLabel.trim().toUpperCase(Locale.ROOT);
        if ("АВАРИЙНОЕ".equals(n) || "DAMAGED".equals(n) || "CRITICAL".equals(n)
                || "КРИТИЧЕСКОЕ".equals(n) || "КРИТИЧНЫЙ".equals(n) || "АВАРИЙНЫЙ".equals(n)) {
            return true;
        }
        return n.contains("АВАРИЙ");
    }
}
