package com.example.rentalservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.domain.Equipment;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.domain.WearLedgerEntryType;
import com.example.rentalservice.repository.DefectReportRepository;
import com.example.rentalservice.repository.EquipmentRepository;
import com.example.rentalservice.repository.EquipmentWearLedgerRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipmentWearAccumulationServiceTest {

    @Mock
    private EquipmentWearLedgerRepository wearLedgerRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private DefectReportRepository defectReportRepository;

    @InjectMocks
    private EquipmentWearAccumulationService service;

    @Test
    void skipsRentalWhenLedgerAlreadyExists() {
        when(wearLedgerRepository.existsByEntryTypeAndServiceRequest_Id(WearLedgerEntryType.RENTAL_CLOSURE, 9L))
                .thenReturn(true);

        service.applyRentalWearForCompletedRequest(9L);

        verify(equipmentRepository, never()).save(any());
        verify(wearLedgerRepository, never()).save(any());
    }

    @Test
    void appliesRentalWearForCompletedRequest() {
        when(wearLedgerRepository.existsByEntryTypeAndServiceRequest_Id(WearLedgerEntryType.RENTAL_CLOSURE, 1L))
                .thenReturn(false);

        Equipment equipment = Mockito.mock(Equipment.class);
        when(equipment.getId()).thenReturn(100L);
        when(equipment.getAccumulatedWearPercent()).thenReturn(new BigDecimal("10.00"));
        when(equipment.getRentalWearRatePerDay()).thenReturn(new BigDecimal("0.1000"));

        ServiceRequest sr = Mockito.mock(ServiceRequest.class);
        when(sr.getStatus()).thenReturn(ServiceRequestStatus.COMPLETED);
        when(sr.getRentalStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(sr.getRentalEndDate()).thenReturn(LocalDate.of(2026, 1, 3));
        when(sr.getEquipment()).thenReturn(equipment);

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(sr));
        when(equipmentRepository.findById(100L)).thenReturn(Optional.of(equipment));

        service.applyRentalWearForCompletedRequest(1L);

        verify(equipmentRepository).save(equipment);
        verify(equipment).setAccumulatedWearPercent(new BigDecimal("10.30"));
        verify(wearLedgerRepository).save(any());
    }
}
