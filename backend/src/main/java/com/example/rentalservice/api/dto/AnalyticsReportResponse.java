package com.example.rentalservice.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalyticsReportResponse(
        String fromDate,
        String toDate,
        long requestsCreated,
        long invoicesIssued,
        long invoicesPaid,
        BigDecimal totalRevenue,
        BigDecimal averagePaidCheck,
        double paymentConversionPercent,
        List<DailyAnalyticsPoint> daily,
        Map<String, Long> requestStatusBreakdown,
        List<TopServiceAnalyticsRow> topServices
) {
    public record DailyAnalyticsPoint(
            String date,
            BigDecimal revenue,
            long requestsCreated,
            long paidInvoices
    ) {}

    public record TopServiceAnalyticsRow(
            Long serviceId,
            String serviceTitle,
            long requestsCount,
            BigDecimal revenue
    ) {}
}
