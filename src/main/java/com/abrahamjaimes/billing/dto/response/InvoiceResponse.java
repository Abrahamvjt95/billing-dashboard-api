package com.abrahamjaimes.billing.dto.response;

import com.abrahamjaimes.billing.entity.Invoice;
import com.abrahamjaimes.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        InvoiceStatus status,
        ClientResponse client,
        LocalDate issueDate,
        LocalDate dueDate,
        String notes,
        BigDecimal subtotal,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal total,
        List<InvoiceItemResponse> items,
        List<PaymentResponse> payments,
        LocalDateTime createdAt
) {
    public static InvoiceResponse from(Invoice inv) {
        return new InvoiceResponse(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getStatus(),
                ClientResponse.from(inv.getClient()),
                inv.getIssueDate(),
                inv.getDueDate(),
                inv.getNotes(),
                inv.getSubtotal(),
                inv.getTaxRate(),
                inv.getTaxAmount(),
                inv.getTotal(),
                inv.getItems().stream().map(InvoiceItemResponse::from).toList(),
                inv.getPayments().stream().map(PaymentResponse::from).toList(),
                inv.getCreatedAt()
        );
    }
}
