package com.abrahamjaimes.billing.controller;

import com.abrahamjaimes.billing.dto.request.PaymentRequest;
import com.abrahamjaimes.billing.dto.response.PaymentResponse;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/payments")
@Tag(name = "Payments", description = "Record and list payments against invoices")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "List all payments for an invoice")
    public List<PaymentResponse> list(@AuthenticationPrincipal User user, @PathVariable Long invoiceId) {
        return paymentService.findByInvoice(user, invoiceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a payment against an invoice (must be SENT or OVERDUE)")
    public PaymentResponse create(@AuthenticationPrincipal User user, @PathVariable Long invoiceId,
                                  @Valid @RequestBody PaymentRequest request) {
        return paymentService.create(user, invoiceId, request);
    }
}
