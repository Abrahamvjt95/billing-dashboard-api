package com.abrahamjaimes.billing.dto.response;

import com.abrahamjaimes.billing.entity.InvoiceItem;

import java.math.BigDecimal;

public record InvoiceItemResponse(Long id, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
    public static InvoiceItemResponse from(InvoiceItem item) {
        return new InvoiceItemResponse(item.getId(), item.getDescription(), item.getQuantity(), item.getUnitPrice(), item.getAmount());
    }
}
