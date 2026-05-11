package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.request.PaymentRequest;
import com.abrahamjaimes.billing.dto.response.PaymentResponse;
import com.abrahamjaimes.billing.entity.Invoice;
import com.abrahamjaimes.billing.entity.InvoiceStatus;
import com.abrahamjaimes.billing.entity.Payment;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.exception.BusinessException;
import com.abrahamjaimes.billing.exception.NotFoundException;
import com.abrahamjaimes.billing.repository.InvoiceRepository;
import com.abrahamjaimes.billing.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByInvoice(User owner, Long invoiceId) {
        getInvoiceOrThrow(owner, invoiceId);
        return paymentRepository.findAllByInvoiceId(invoiceId)
                .stream().map(PaymentResponse::from).toList();
    }

    public PaymentResponse create(User owner, Long invoiceId, PaymentRequest request) {
        Invoice invoice = getInvoiceOrThrow(owner, invoiceId);

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessException("Cannot register payment on a DRAFT invoice");
        }

        BigDecimal alreadyPaid = paymentRepository.sumAmountByInvoiceId(invoiceId);
        BigDecimal remaining = invoice.getTotal().subtract(alreadyPaid);

        if (request.amount().compareTo(remaining) > 0) {
            throw new BusinessException("Payment amount exceeds remaining balance of " + remaining);
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(request.amount())
                .paymentDate(request.paymentDate())
                .method(request.method())
                .reference(request.reference())
                .notes(request.notes())
                .build();

        payment = paymentRepository.save(payment);

        BigDecimal totalPaid = alreadyPaid.add(request.amount());
        if (totalPaid.compareTo(invoice.getTotal()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        }

        return PaymentResponse.from(payment);
    }

    private Invoice getInvoiceOrThrow(User owner, Long invoiceId) {
        return invoiceRepository.findByIdAndOwnerId(invoiceId, owner.getId())
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));
    }
}
