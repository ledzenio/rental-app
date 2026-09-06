package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.CreateServiceRequestPayload;
import com.example.rentalservice.api.dto.EquipmentOptionResponse;
import com.example.rentalservice.api.dto.SavedServiceResponse;
import com.example.rentalservice.api.dto.ServiceRequestResponse;
import com.example.rentalservice.api.dto.UpdateServiceRequestPayload;
import com.example.rentalservice.domain.Equipment;
import com.example.rentalservice.domain.SavedService;
import com.example.rentalservice.domain.EquipmentLifecycleStatus;
import com.example.rentalservice.domain.ServiceCatalogItem;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.domain.InvoiceStatus;
import com.example.rentalservice.repository.EquipmentRepository;
import com.example.rentalservice.repository.InvoiceRepository;
import com.example.rentalservice.repository.SavedServiceRepository;
import com.example.rentalservice.repository.ServiceCatalogRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceFlowService {

    private final SavedServiceRepository savedServiceRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final GeocodingService geocodingService;
    private final InvoiceRepository invoiceRepository;
    private final EquipmentRepository equipmentRepository;
    private final ServiceRequestCompletionService serviceRequestCompletionService;

    public UserServiceFlowService(
            SavedServiceRepository savedServiceRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            ServiceRequestRepository serviceRequestRepository,
            GeocodingService geocodingService,
            InvoiceRepository invoiceRepository,
            EquipmentRepository equipmentRepository,
            ServiceRequestCompletionService serviceRequestCompletionService
    ) {
        this.savedServiceRepository = savedServiceRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.geocodingService = geocodingService;
        this.invoiceRepository = invoiceRepository;
        this.equipmentRepository = equipmentRepository;
        this.serviceRequestCompletionService = serviceRequestCompletionService;
    }

    @Transactional
    public void addToSaved(User user, Long serviceId) {
        if (savedServiceRepository.existsByUserIdAndServiceId(user.getId(), serviceId)) {
            return;
        }
        ServiceCatalogItem item = loadActiveService(serviceId);
        SavedService savedService = new SavedService();
        savedService.setUser(user);
        savedService.setService(item);
        savedService.setCreatedAt(Instant.now());
        savedServiceRepository.save(savedService);
    }

    @Transactional
    public void removeFromSaved(User user, Long serviceId) {
        savedServiceRepository.deleteByUserIdAndServiceId(user.getId(), serviceId);
    }

    @Transactional(readOnly = true)
    public List<SavedServiceResponse> getSaved(User user) {
        return savedServiceRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(saved -> new SavedServiceResponse(
                        saved.getService().getId(),
                        saved.getService().getTitle(),
                        saved.getService().getCategory(),
                        saved.getService().getBasePrice(),
                        saved.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public ServiceRequestResponse createRequest(User user, CreateServiceRequestPayload payload) {
        if (invoiceRepository.existsByUser_IdAndStatus(user.getId(), InvoiceStatus.ISSUED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нельзя оформить новую заявку, пока есть неоплаченные счета. Оплатите их в разделе «Мои счета»."
            );
        }
        ServiceCatalogItem service = loadActiveService(payload.serviceId());
        if ("Сервис".equalsIgnoreCase(service.getCategory()) || "Диагностика и обслуживание".equalsIgnoreCase(service.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заявка должна оформляться только на оборудование из каталога.");
        }
        if (payload.rentalEndDate().isBefore(payload.rentalStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата окончания аренды не может быть раньше даты начала.");
        }
        // Только AVAILABLE: в ремонте (UNDER_REPAIR) и помеченное как NEEDS_REPAIR в выдачу не попадают.
        var matchingEquipment = equipmentRepository.findAllByServiceCatalogItemIdAndLifecycleStatusOrderByIdAsc(
                service.getId(),
                EquipmentLifecycleStatus.AVAILABLE
        );
        var equipment = matchingEquipment.stream()
                .filter(e -> e.getLifecycleStatus() == EquipmentLifecycleStatus.AVAILABLE)
                .filter(e -> !serviceRequestRepository.existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqual(
                        e.getId(),
                        Set.of(
                                ServiceRequestStatus.NEW,
                                ServiceRequestStatus.AWAITING_PAYMENT,
                                ServiceRequestStatus.IN_PROGRESS,
                                ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW
                        ),
                        payload.rentalEndDate(),
                        payload.rentalStartDate()
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Нет доступной единицы оборудования по выбранной услуге в указанный период."
                ));
        ServiceRequest request = new ServiceRequest();
        request.setUser(user);
        request.setService(service);
        request.setEquipment(equipment);
        request.setRentalStartDate(payload.rentalStartDate());
        request.setRentalEndDate(payload.rentalEndDate());
        request.setShiftsPerDay(1);
        String notes = payload.notes();
        var estimate = geocodingService.estimateForAddress(payload.objectAddress().trim());
        String logisticsBlock = "Адрес объекта: " + estimate.normalizedAddress()
                + " | Координаты: " + estimate.latitude() + ", " + estimate.longitude()
                + " | Дистанция от базы: " + estimate.distanceKm() + " км"
                + " | Логистическая надбавка: " + estimate.logisticsSurcharge() + " BYN";
        if (notes == null || notes.isBlank()) {
            notes = logisticsBlock;
        } else {
            notes = notes.trim() + "\n" + logisticsBlock;
        }
        request.setNotes(notes);
        request.setStatus(ServiceRequestStatus.NEW);
        request.setCreatedAt(Instant.now());
        ServiceRequest saved = serviceRequestRepository.save(request);
        return toResponse(saved);
    }

    @Transactional
    public List<ServiceRequestResponse> getMyRequests(User user) {
        List<ServiceRequest> list = serviceRequestRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        boolean dirty = false;
        for (ServiceRequest r : list) {
            if (r.getReturnedAt() != null && r.getStatus() == ServiceRequestStatus.IN_PROGRESS) {
                r.setStatus(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW);
                dirty = true;
            }
        }
        if (dirty) {
            serviceRequestRepository.saveAll(list);
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ServiceRequestResponse updateRequest(User user, Long requestId, UpdateServiceRequestPayload payload) {
        ServiceRequest request = serviceRequestRepository.findByIdAndUserId(requestId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() == ServiceRequestStatus.COMPLETED || request.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only active requests can be edited");
        }
        if (request.getStatus() != ServiceRequestStatus.NEW && request.getStatus() != ServiceRequestStatus.AWAITING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Заявку в работе или после возврата нельзя редактировать: условия зафиксированы после оплаты."
            );
        }
        if (payload.rentalStartDate() == null || payload.rentalEndDate() == null || payload.rentalEndDate().isBefore(payload.rentalStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата окончания аренды не может быть раньше даты начала.");
        }
        if (payload.objectAddress() == null || payload.objectAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите адрес объекта для перерасчета логистики.");
        }
        var estimate = geocodingService.estimateForAddress(payload.objectAddress().trim());
        String logisticsBlock = "Адрес объекта: " + estimate.normalizedAddress()
                + " | Координаты: " + estimate.latitude() + ", " + estimate.longitude()
                + " | Дистанция от базы: " + estimate.distanceKm() + " км"
                + " | Логистическая надбавка: " + estimate.logisticsSurcharge() + " BYN";

        Long currentEquipmentId = request.getEquipment().getId();
        Set<ServiceRequestStatus> activeStatuses = Set.of(
                ServiceRequestStatus.NEW,
                ServiceRequestStatus.AWAITING_PAYMENT,
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW
        );
        boolean currentEquipmentBusy = serviceRequestRepository
                .existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqualAndIdNot(
                        currentEquipmentId,
                        activeStatuses,
                        payload.rentalEndDate(),
                        payload.rentalStartDate(),
                        request.getId()
                );
        if (currentEquipmentBusy) {
            Equipment replacement = equipmentRepository
                    .findAllByServiceCatalogItemIdAndLifecycleStatusOrderByIdAsc(
                            request.getService().getId(),
                            EquipmentLifecycleStatus.AVAILABLE
                    )
                    .stream()
                    .filter(e -> e.getLifecycleStatus() == EquipmentLifecycleStatus.AVAILABLE)
                    .filter(e -> !serviceRequestRepository
                            .existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqualAndIdNot(
                                    e.getId(),
                                    activeStatuses,
                                    payload.rentalEndDate(),
                                    payload.rentalStartDate(),
                                    request.getId()
                            ))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Нет доступной единицы оборудования на новый период."
                    ));
            request.setEquipment(replacement);
        }

        request.setRentalStartDate(payload.rentalStartDate());
        request.setRentalEndDate(payload.rentalEndDate());
        request.setNotes(mergeOriginalCommentWithLogistics(request.getNotes(), logisticsBlock));
        return toResponse(serviceRequestRepository.save(request));
    }

    @Transactional
    public ServiceRequestResponse cancelRequest(User user, Long requestId) {
        ServiceRequest request = serviceRequestRepository.findByIdAndUserId(requestId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() == ServiceRequestStatus.COMPLETED || request.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request cannot be cancelled");
        }
        if (request.getStatus() != ServiceRequestStatus.NEW && request.getStatus() != ServiceRequestStatus.AWAITING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Отозвать заявку можно только до оплаты и выезда: после этого используйте сценарий возврата оборудования и заключения сервиса."
            );
        }
        request.setStatus(ServiceRequestStatus.CANCELLED);
        return toResponse(serviceRequestRepository.save(request));
    }

    @Transactional
    public ServiceRequestResponse markReturned(User user, Long requestId) {
        ServiceRequest request = serviceRequestRepository.findByIdAndUserId(requestId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getReturnedAt() != null) {
            if (request.getStatus() == ServiceRequestStatus.IN_PROGRESS) {
                request.setStatus(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW);
                ServiceRequest fixed = serviceRequestRepository.save(request);
                serviceRequestCompletionService.tryAutoComplete(fixed.getId());
                fixed = serviceRequestRepository.findById(fixed.getId()).orElse(fixed);
                return toResponse(fixed);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Возврат уже зафиксирован.");
        }
        if (request.getStatus() != ServiceRequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Возврат можно отметить только для заявки в работе.");
        }
        request.setReturnedAt(Instant.now());
        request.setStatus(ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW);
        ServiceRequest savedReq = serviceRequestRepository.save(request);
        serviceRequestCompletionService.tryAutoComplete(savedReq.getId());
        savedReq = serviceRequestRepository.findById(savedReq.getId()).orElse(savedReq);
        return toResponse(savedReq);
    }

    @Transactional(readOnly = true)
    public List<EquipmentOptionResponse> listAvailableEquipment(Long serviceId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите корректный период подбора оборудования.");
        }
        List<Equipment> baseSet = serviceId == null
                ? equipmentRepository.findAll()
                : equipmentRepository.findAllByServiceCatalogItemIdAndLifecycleStatusOrderByIdAsc(
                        serviceId,
                        EquipmentLifecycleStatus.AVAILABLE
                );
        return baseSet.stream()
                .filter(e -> e.getLifecycleStatus() == EquipmentLifecycleStatus.AVAILABLE)
                .filter(e -> !serviceRequestRepository.existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqual(
                        e.getId(),
                        Set.of(
                                ServiceRequestStatus.NEW,
                                ServiceRequestStatus.AWAITING_PAYMENT,
                                ServiceRequestStatus.IN_PROGRESS,
                                ServiceRequestStatus.AWAITING_SPECIALIST_REVIEW
                        ),
                        toDate,
                        fromDate
                ))
                .map(e -> new EquipmentOptionResponse(
                        e.getId(),
                        e.getInventoryCode(),
                        e.getModelName(),
                        e.getAccumulatedWearPercent(),
                        e.getRentalWearRatePerDay()
                ))
                .toList();
    }

    @Transactional
    public void deleteHistoryRequest(User user, Long requestId) {
        ServiceRequest request = serviceRequestRepository.findByIdAndUserId(requestId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() != ServiceRequestStatus.CANCELLED && request.getStatus() != ServiceRequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed or cancelled requests can be removed from history");
        }
        if (invoiceRepository.existsByServiceRequestId(requestId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request has invoices and cannot be deleted from history");
        }
        serviceRequestRepository.delete(request);
    }

    @Transactional
    public int clearHistory(User user) {
        List<ServiceRequest> items = serviceRequestRepository.findAllByUserIdAndStatusInOrderByCreatedAtDesc(
                user.getId(),
                Set.of(ServiceRequestStatus.CANCELLED, ServiceRequestStatus.COMPLETED)
        );
        int deleted = 0;
        for (ServiceRequest item : items) {
            if (!invoiceRepository.existsByServiceRequestId(item.getId())) {
                serviceRequestRepository.delete(item);
                deleted++;
            }
        }
        return deleted;
    }

    private ServiceCatalogItem loadActiveService(Long serviceId) {
        ServiceCatalogItem item = serviceCatalogRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        if (!item.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service is not active");
        }
        return item;
    }

    private String mergeOriginalCommentWithLogistics(String existingNotes, String logisticsBlock) {
        if (existingNotes == null || existingNotes.isBlank()) {
            return logisticsBlock;
        }
        int idx = existingNotes.indexOf("Адрес объекта:");
        String userComment = idx >= 0 ? existingNotes.substring(0, idx).trim() : existingNotes.trim();
        if (userComment.isBlank()) {
            return logisticsBlock;
        }
        return userComment + "\n" + logisticsBlock;
    }

    private ServiceRequestResponse toResponse(ServiceRequest request) {
        int rentalDays = (int) (ChronoUnit.DAYS.between(request.getRentalStartDate(), request.getRentalEndDate()) + 1);
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
                false
        );
    }
}
