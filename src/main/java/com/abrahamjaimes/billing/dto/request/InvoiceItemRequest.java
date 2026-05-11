package com.abrahamjaimes.billing.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotBlank String description,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {}
