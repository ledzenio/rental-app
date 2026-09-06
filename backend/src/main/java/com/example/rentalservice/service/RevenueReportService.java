package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.AnalyticsReportResponse;
import com.example.rentalservice.api.dto.RevenueReportResponse;
import com.example.rentalservice.domain.Invoice;
import com.example.rentalservice.domain.InvoiceStatus;
import com.example.rentalservice.domain.InvoiceType;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.repository.InvoiceRepository;
import com.example.rentalservice.repository.ServiceRequestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueReportService {

    private final InvoiceRepository invoiceRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public RevenueReportService(InvoiceRepository invoiceRepository, ServiceRequestRepository serviceRequestRepository) {
        this.invoiceRepository = invoiceRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse build(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Invoice> paid = invoiceRepository.findAllByStatusAndPaidAtBetweenOrderByPaidAtAsc(InvoiceStatus.PAID, from, to);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal service = BigDecimal.ZERO;
        BigDecimal penalty = BigDecimal.ZERO;
        for (Invoice invoice : paid) {
            total = total.add(invoice.getAmount());
            if (invoice.getInvoiceType() == InvoiceType.SERVICE) {
                service = service.add(invoice.getAmount());
            } else if (invoice.getInvoiceType() == InvoiceType.PENALTY) {
                penalty = penalty.add(invoice.getAmount());
            }
        }
        return new RevenueReportResponse(
                fromDate.toString(),
                toDate.toString(),
                paid.size(),
                total,
                service,
                penalty
        );
    }

    @Transactional(readOnly = true)
    public String buildCsv(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        RevenueReportResponse report = build(fromDate, toDate);
        return "fromDate,toDate,paidInvoices,totalRevenue,serviceRevenue,penaltyRevenue\n"
                + report.fromDate() + ","
                + report.toDate() + ","
                + report.paidInvoices() + ","
                + report.totalRevenue() + ","
                + report.serviceRevenue() + ","
                + report.penaltyRevenue() + "\n";
    }

    @Transactional(readOnly = true)
    public AnalyticsReportResponse buildAnalytics(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<ServiceRequest> requests = serviceRequestRepository.findAllByCreatedAtBetween(from, to);
        List<Invoice> issuedInvoices = invoiceRepository.findAllByCreatedAtBetween(from, to);
        List<Invoice> paidInvoices = invoiceRepository.findAllByStatusAndPaidAtBetweenOrderByPaidAtAsc(InvoiceStatus.PAID, from, to);

        Map<LocalDate, BigDecimal> revenueByDay = new HashMap<>();
        Map<LocalDate, Long> requestsByDay = new HashMap<>();
        Map<LocalDate, Long> paidInvoicesByDay = new HashMap<>();

        for (ServiceRequest request : requests) {
            LocalDate day = request.getCreatedAt().atOffset(ZoneOffset.UTC).toLocalDate();
            requestsByDay.put(day, requestsByDay.getOrDefault(day, 0L) + 1);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Invoice invoice : paidInvoices) {
            LocalDate day = invoice.getPaidAt().atOffset(ZoneOffset.UTC).toLocalDate();
            paidInvoicesByDay.put(day, paidInvoicesByDay.getOrDefault(day, 0L) + 1);
            revenueByDay.put(day, revenueByDay.getOrDefault(day, BigDecimal.ZERO).add(invoice.getAmount()));
            totalRevenue = totalRevenue.add(invoice.getAmount());
        }

        List<AnalyticsReportResponse.DailyAnalyticsPoint> daily = new ArrayList<>();
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            daily.add(new AnalyticsReportResponse.DailyAnalyticsPoint(
                    cursor.toString(),
                    revenueByDay.getOrDefault(cursor, BigDecimal.ZERO),
                    requestsByDay.getOrDefault(cursor, 0L),
                    paidInvoicesByDay.getOrDefault(cursor, 0L)
            ));
            cursor = cursor.plusDays(1);
        }

        Map<ServiceRequestStatus, Long> statusCounts = new EnumMap<>(ServiceRequestStatus.class);
        for (ServiceRequest request : requests) {
            statusCounts.put(request.getStatus(), statusCounts.getOrDefault(request.getStatus(), 0L) + 1);
        }
        Map<String, Long> statusBreakdown = new HashMap<>();
        for (ServiceRequestStatus status : ServiceRequestStatus.values()) {
            statusBreakdown.put(status.name(), statusCounts.getOrDefault(status, 0L));
        }

        Map<Long, AnalyticsReportResponse.TopServiceAnalyticsRow> topMap = new HashMap<>();
        for (ServiceRequest request : requests) {
            Long serviceId = request.getService().getId();
            AnalyticsReportResponse.TopServiceAnalyticsRow prev = topMap.get(serviceId);
            BigDecimal rev = prev == null ? BigDecimal.ZERO : prev.revenue();
            long cnt = prev == null ? 0 : prev.requestsCount();
            topMap.put(serviceId, new AnalyticsReportResponse.TopServiceAnalyticsRow(
                    serviceId,
                    request.getService().getTitle(),
                    cnt + 1,
                    rev
            ));
        }
        for (Invoice invoice : paidInvoices) {
            if (invoice.getServiceRequest() == null || invoice.getServiceRequest().getService() == null) {
                continue;
            }
            Long serviceId = invoice.getServiceRequest().getService().getId();
            AnalyticsReportResponse.TopServiceAnalyticsRow prev = topMap.get(serviceId);
            if (prev == null) {
                topMap.put(serviceId, new AnalyticsReportResponse.TopServiceAnalyticsRow(
                        serviceId,
                        invoice.getServiceRequest().getService().getTitle(),
                        0L,
                        invoice.getAmount()
                ));
            } else {
                topMap.put(serviceId, new AnalyticsReportResponse.TopServiceAnalyticsRow(
                        prev.serviceId(),
                        prev.serviceTitle(),
                        prev.requestsCount(),
                        prev.revenue().add(invoice.getAmount())
                ));
            }
        }
        List<AnalyticsReportResponse.TopServiceAnalyticsRow> topServices = topMap.values().stream()
                .sorted((a, b) -> b.revenue().compareTo(a.revenue()))
                .limit(10)
                .toList();

        BigDecimal avgCheck = paidInvoices.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(paidInvoices.size()), 2, java.math.RoundingMode.HALF_UP);
        double conversion = issuedInvoices.isEmpty()
                ? 0.0
                : (paidInvoices.size() * 100.0) / issuedInvoices.size();

        return new AnalyticsReportResponse(
                fromDate.toString(),
                toDate.toString(),
                requests.size(),
                issuedInvoices.size(),
                paidInvoices.size(),
                totalRevenue,
                avgCheck,
                Math.round(conversion * 100.0) / 100.0,
                daily,
                statusBreakdown,
                topServices
        );
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate и toDate обязательны.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate не может быть раньше fromDate.");
        }
    }
}
