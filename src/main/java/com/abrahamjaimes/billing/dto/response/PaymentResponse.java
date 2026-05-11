package com.abrahamjaimes.billing.dto.response;

import com.abrahamjaimes.billing.entity.Payment;
import com.abrahamjaimes.billing.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(Long id, BigDecimal amount, LocalDate paymentDate, PaymentMethod method, String reference, String notes, LocalDateTime createdAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getAmount(), p.getPaymentDate(), p.getMethod(), p.getReference(), p.getNotes(), p.getCreatedAt());
    }
}
