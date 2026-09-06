package com.example.rentalservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private DefectReportRepository defectReportRepository;
    @Mock
    private MailService mailService;
    @Mock
    private ServiceRequestCompletionService serviceRequestCompletionService;

    @InjectMocks
    private BillingService billingService;

    @Test
    void payInvoiceRejectsInsufficientBalance() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 10L);
        user.setVirtualBalance(new BigDecimal("10.00"));

        Invoice invoice = new Invoice();
        invoice.setUser(user);
        invoice.setAmount(new BigDecimal("100.00"));
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setInvoiceType(InvoiceType.SERVICE);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> billingService.payInvoice(user, 1L)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void payInvoiceMarksAsPaidAndMovesServiceRequestToInProgress() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 11L);
        user.setVirtualBalance(new BigDecimal("500.00"));

        ServiceRequest request = new ServiceRequest();
        request.setStatus(ServiceRequestStatus.AWAITING_PAYMENT);

        Invoice invoice = new Invoice();
        invoice.setUser(user);
        invoice.setServiceRequest(request);
        invoice.setInvoiceType(InvoiceType.SERVICE);
        invoice.setAmount(new BigDecimal("100.00"));
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setCreatedAt(Instant.now());

        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(invoice));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.payInvoice(user, 2L);

        assertEquals(InvoiceStatus.PAID, response.status());
        assertEquals(ServiceRequestStatus.IN_PROGRESS, request.getStatus());
        assertEquals(new BigDecimal("400.00"), user.getVirtualBalance());
        verify(invoiceRepository).save(invoice);
    }
}
