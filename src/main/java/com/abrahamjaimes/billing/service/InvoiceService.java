package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.request.InvoiceItemRequest;
import com.abrahamjaimes.billing.dto.request.InvoiceRequest;
import com.abrahamjaimes.billing.dto.response.InvoiceResponse;
import com.abrahamjaimes.billing.dto.response.PageResponse;
import com.abrahamjaimes.billing.entity.*;
import com.abrahamjaimes.billing.exception.BusinessException;
import com.abrahamjaimes.billing.exception.ConflictException;
import com.abrahamjaimes.billing.exception.NotFoundException;
import com.abrahamjaimes.billing.repository.ClientRepository;
import com.abrahamjaimes.billing.repository.InvoiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, ClientRepository clientRepository) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> findAll(User owner, InvoiceStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var invoicePage = status != null
                ? invoiceRepository.findAllByOwnerIdAndStatus(owner.getId(), status, pageable)
                : invoiceRepository.findAllByOwnerId(owner.getId(), pageable);
        return PageResponse.from(invoicePage, InvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(User owner, Long id) {
        return InvoiceResponse.from(getInvoiceOrThrow(owner, id));
    }

    public InvoiceResponse create(User owner, InvoiceRequest request) {
        if (invoiceRepository.existsByOwnerIdAndInvoiceNumber(owner.getId(), request.invoiceNumber())) {
            throw new ConflictException("Invoice number already exists: " + request.invoiceNumber());
        }

        Client client = clientRepository.findByIdAndOwnerId(request.clientId(), owner.getId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + request.clientId()));

        Invoice invoice = Invoice.builder()
                .owner(owner)
                .client(client)
                .invoiceNumber(request.invoiceNumber())
                .status(request.status() != null ? request.status() : InvoiceStatus.DRAFT)
                .issueDate(request.issueDate())
                .dueDate(request.dueDate())
                .notes(request.notes())
                .taxRate(request.taxRate())
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        addItemsToInvoice(invoice, request.items());
        invoice.recalculateTotals();

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    public InvoiceResponse update(User owner, Long id, InvoiceRequest request) {
        Invoice invoice = getInvoiceOrThrow(owner, id);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Cannot edit a paid invoice");
        }

        if (!invoice.getInvoiceNumber().equals(request.invoiceNumber())
                && invoiceRepository.existsByOwnerIdAndInvoiceNumber(owner.getId(), request.invoiceNumber())) {
            throw new ConflictException("Invoice number already exists: " + request.invoiceNumber());
        }

        Client client = clientRepository.findByIdAndOwnerId(request.clientId(), owner.getId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + request.clientId()));

        invoice.setClient(client);
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setStatus(request.status() != null ? request.status() : invoice.getStatus());
        invoice.setIssueDate(request.issueDate());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());
        invoice.setTaxRate(request.taxRate());
        invoice.getItems().clear();
        addItemsToInvoice(invoice, request.items());
        invoice.recalculateTotals();

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    public void delete(User owner, Long id) {
        Invoice invoice = getInvoiceOrThrow(owner, id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Cannot delete a paid invoice");
        }
        invoiceRepository.delete(invoice);
    }

    private void addItemsToInvoice(Invoice invoice, List<InvoiceItemRequest> itemRequests) {
        for (int i = 0; i < itemRequests.size(); i++) {
            InvoiceItemRequest req = itemRequests.get(i);
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .description(req.description())
                    .quantity(req.quantity())
                    .unitPrice(req.unitPrice())
                    .sortOrder(i)
                    .build();
            item.calculateAmount();
            invoice.getItems().add(item);
        }
    }

    private Invoice getInvoiceOrThrow(User owner, Long id) {
        return invoiceRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
    }
}
