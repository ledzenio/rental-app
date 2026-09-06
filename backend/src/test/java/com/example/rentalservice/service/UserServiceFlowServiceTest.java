package com.example.rentalservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.rentalservice.api.dto.CreateServiceRequestPayload;
import com.example.rentalservice.api.dto.GeocodingEstimateResponse;
import com.example.rentalservice.api.dto.ServiceRequestResponse;
import com.example.rentalservice.domain.Equipment;
import com.example.rentalservice.domain.EquipmentLifecycleStatus;
import com.example.rentalservice.domain.InvoiceStatus;
import com.example.rentalservice.domain.ServiceCatalogItem;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.repository.EquipmentRepository;
import com.example.rentalservice.repository.InvoiceRepository;
import com.example.rentalservice.repository.SavedServiceRepository;
import com.example.rentalservice.repository.ServiceCatalogRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceFlowServiceTest {

    @Mock
    private SavedServiceRepository savedServiceRepository;
    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private GeocodingService geocodingService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private ServiceRequestCompletionService serviceRequestCompletionService;

    @InjectMocks
    private UserServiceFlowService service;

    @Test
    void createRequestRejectsWhenUserHasIssuedInvoices() {
        User user = Mockito.mock(User.class);
        when(user.getId()).thenReturn(15L);
        when(invoiceRepository.existsByUser_IdAndStatus(15L, InvoiceStatus.ISSUED)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createRequest(user, new CreateServiceRequestPayload(
                        1L,
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 2),
                        "Комментарий",
                        "Минск, Немига 1"
                ))
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createRequestCreatesNewRequestWhenEquipmentAvailable() {
        User user = Mockito.mock(User.class);
        when(user.getId()).thenReturn(20L);
        when(user.getEmail()).thenReturn("client@test.local");
        when(invoiceRepository.existsByUser_IdAndStatus(20L, InvoiceStatus.ISSUED)).thenReturn(false);

        ServiceCatalogItem serviceItem = Mockito.mock(ServiceCatalogItem.class);
        when(serviceItem.getId()).thenReturn(5L);
        when(serviceItem.isActive()).thenReturn(true);
        when(serviceItem.getCategory()).thenReturn("Строительное оборудование");
        when(serviceItem.getTitle()).thenReturn("Аренда генератора");
        when(serviceItem.getBasePrice()).thenReturn(new BigDecimal("2200.00"));
        when(serviceCatalogRepository.findById(5L)).thenReturn(Optional.of(serviceItem));

        Equipment equipment = Mockito.mock(Equipment.class);
        when(equipment.getId()).thenReturn(101L);
        when(equipment.getLifecycleStatus()).thenReturn(EquipmentLifecycleStatus.AVAILABLE);
        when(equipment.getInventoryCode()).thenReturn("CNSTR-047-1");
        when(equipment.getModelName()).thenReturn("Atlas Copco 100 кВт");
        when(equipmentRepository.findAllByServiceCatalogItemIdAndLifecycleStatusOrderByIdAsc(
                5L, EquipmentLifecycleStatus.AVAILABLE
        )).thenReturn(List.of(equipment));
        when(serviceRequestRepository.existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqual(
                any(), any(), any(), any()
        )).thenReturn(false);
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(geocodingService.estimateForAddress("Минск, Немига 1")).thenReturn(
                new GeocodingEstimateResponse(
                        "Минск, Немига 1",
                        "г. Минск, ул. Немига, 1",
                        53.90,
                        27.56,
                        7.8,
                        5.6
                )
        );

        ServiceRequestResponse response = service.createRequest(user, new CreateServiceRequestPayload(
                5L,
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 12),
                "Нужна доставка утром",
                "Минск, Немига 1"
        ));

        assertEquals("Аренда генератора", response.serviceTitle());
        assertEquals(101L, response.equipmentId());
        assertEquals("NEW", response.status().name());
        verify(serviceRequestRepository).save(any(ServiceRequest.class));
    }
}
