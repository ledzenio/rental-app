package com.example.rentalservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.rentalservice.api.dto.ManagerDefectStatusUpdateRequest;
import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectReportStatus;
import com.example.rentalservice.repository.DefectReportRepository;
import com.example.rentalservice.repository.InvoiceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class ManagerDefectWorkflowServiceTest {

    @Test
    void shouldRejectIllegalTransitionFromApprovedToRejected() {
        DefectReportRepository repository = Mockito.mock(DefectReportRepository.class);
        ManagerDefectWorkflowService service = new ManagerDefectWorkflowService(
                repository,
                Mockito.mock(BillingService.class),
                Mockito.mock(InvoiceRepository.class),
                Mockito.mock(ServiceRequestCompletionService.class),
                Mockito.mock(EquipmentWearAccumulationService.class)
        );

        DefectReport report = new DefectReport();
        report.setStatus(DefectReportStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(report));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.updateStatus(1L, new ManagerDefectStatusUpdateRequest(DefectReportStatus.REJECTED))
        );
        assertEquals(400, ex.getStatusCode().value());
    }
}
