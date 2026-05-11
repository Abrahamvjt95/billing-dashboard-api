package com.abrahamjaimes.billing.dto.response;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        long totalClients,
        long totalInvoices,
        long draftInvoices,
        long sentInvoices,
        long paidInvoices,
        long overdueInvoices,
        BigDecimal totalRevenue,
        BigDecimal pendingRevenue,
        BigDecimal collectedRevenue
) {}
