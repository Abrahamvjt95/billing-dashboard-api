package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.request.PaymentRequest;
import com.abrahamjaimes.billing.entity.*;
import com.abrahamjaimes.billing.exception.BusinessException;
import com.abrahamjaimes.billing.repository.InvoiceRepository;
import com.abrahamjaimes.billing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentRepository paymentRepository;
    @InjectMocks PaymentService paymentService;

    private User owner;
    private Invoice sentInvoice;
    private Invoice draftInvoice;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("owner@test.com")
                .role(Role.USER).enabled(true).build();

        Client client = Client.builder().id(5L).owner(owner).name("Acme").build();

        sentInvoice = Invoice.builder()
                .id(10L).owner(owner).client(client)
                .invoiceNumber("INV-001").status(InvoiceStatus.SENT)
                .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(30))
                .subtotal(new BigDecimal("100.00")).taxRate(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO).total(new BigDecimal("100.00"))
                .items(new ArrayList<>()).payments(new ArrayList<>()).build();

        draftInvoice = Invoice.builder()
                .id(20L).owner(owner).client(client)
                .invoiceNumber("INV-002").status(InvoiceStatus.DRAFT)
                .total(new BigDecimal("50.00"))
                .items(new ArrayList<>()).payments(new ArrayList<>()).build();
    }

    @Test
    void create_throwsBusiness_whenInvoiceIsDraft() {
        when(invoiceRepository.findByIdAndOwnerId(20L, 1L)).thenReturn(Optional.of(draftInvoice));

        var request = new PaymentRequest(
                new BigDecimal("50.00"), LocalDate.now(), PaymentMethod.CASH, null, null);

        assertThatThrownBy(() -> paymentService.create(owner, 20L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void create_throwsBusiness_whenAmountExceedsBalance() {
        when(invoiceRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sentInvoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(BigDecimal.ZERO);

        var request = new PaymentRequest(
                new BigDecimal("200.00"), LocalDate.now(), PaymentMethod.BANK_TRANSFER, null, null);

        assertThatThrownBy(() -> paymentService.create(owner, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("remaining balance");
    }

    @Test
    void create_marksInvoicePaid_whenFullyPaid() {
        when(invoiceRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sentInvoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new PaymentRequest(
                new BigDecimal("100.00"), LocalDate.now(), PaymentMethod.BANK_TRANSFER, "TXN-1", null);

        paymentService.create(owner, 10L, request);

        verify(invoiceRepository).save(argThat(inv -> inv.getStatus() == InvoiceStatus.PAID));
    }

    @Test
    void create_doesNotMarkPaid_whenPartialPayment() {
        when(invoiceRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sentInvoice));
        when(paymentRepository.sumAmountByInvoiceId(10L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new PaymentRequest(
                new BigDecimal("40.00"), LocalDate.now(), PaymentMethod.CASH, null, null);

        paymentService.create(owner, 10L, request);

        verify(invoiceRepository, never()).save(any());
    }
}
