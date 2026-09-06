package com.example.rentalservice.api.dto;

import java.math.BigDecimal;

public record RevenueReportResponse(
        String fromDate,
        String toDate,
        long paidInvoices,
        BigDecimal totalRevenue,
        BigDecimal serviceRevenue,
        BigDecimal penaltyRevenue
) {
}
