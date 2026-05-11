package com.abrahamjaimes.billing.dto.request;

import com.abrahamjaimes.billing.entity.InvoiceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        @NotNull Long clientId,
        @NotBlank String invoiceNumber,
        InvoiceStatus status,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        String notes,
        @NotNull @DecimalMin("0") BigDecimal taxRate,
        @NotEmpty @Valid List<InvoiceItemRequest> items
) {}
