package com.abrahamjaimes.billing.controller;

import com.abrahamjaimes.billing.dto.request.InvoiceRequest;
import com.abrahamjaimes.billing.dto.response.InvoiceResponse;
import com.abrahamjaimes.billing.dto.response.PageResponse;
import com.abrahamjaimes.billing.entity.InvoiceStatus;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Create and manage invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @Operation(summary = "List invoices (paginated, optional status filter)")
    public PageResponse<InvoiceResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoiceService.findAll(user, status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public InvoiceResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return invoiceService.findById(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new invoice with line items")
    public InvoiceResponse create(@AuthenticationPrincipal User user, @Valid @RequestBody InvoiceRequest request) {
        return invoiceService.create(user, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an invoice (not allowed if PAID)")
    public InvoiceResponse update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                  @Valid @RequestBody InvoiceRequest request) {
        return invoiceService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an invoice (not allowed if PAID)")
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        invoiceService.delete(user, id);
    }
}
