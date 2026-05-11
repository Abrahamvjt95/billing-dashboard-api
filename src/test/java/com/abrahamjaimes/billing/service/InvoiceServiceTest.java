package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.request.InvoiceItemRequest;
import com.abrahamjaimes.billing.dto.request.InvoiceRequest;
import com.abrahamjaimes.billing.dto.response.InvoiceResponse;
import com.abrahamjaimes.billing.entity.*;
import com.abrahamjaimes.billing.exception.BusinessException;
import com.abrahamjaimes.billing.exception.ConflictException;
import com.abrahamjaimes.billing.exception.NotFoundException;
import com.abrahamjaimes.billing.repository.ClientRepository;
import com.abrahamjaimes.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ClientRepository clientRepository;
    @InjectMocks InvoiceService invoiceService;

    private User owner;
    private Client client;
    private Invoice paidInvoice;
    private InvoiceRequest validRequest;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("owner@test.com")
                .role(Role.USER).enabled(true).build();

        client = Client.builder().id(5L).owner(owner).name("Acme").build();

        paidInvoice = Invoice.builder()
                .id(100L).owner(owner).client(client)
                .invoiceNumber("INV-001").status(InvoiceStatus.PAID)
                .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(30))
                .subtotal(BigDecimal.TEN).taxRate(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO).total(BigDecimal.TEN)
                .items(new ArrayList<>()).payments(new ArrayList<>()).build();

        validRequest = new InvoiceRequest(
                5L, "INV-NEW", InvoiceStatus.DRAFT,
                LocalDate.now(), LocalDate.now().plusDays(30),
                "notes", BigDecimal.ZERO,
                List.of(new InvoiceItemRequest("Service", BigDecimal.ONE, BigDecimal.TEN))
        );
    }

    @Test
    void create_throwsConflict_whenInvoiceNumberExists() {
        when(invoiceRepository.existsByOwnerIdAndInvoiceNumber(1L, "INV-NEW")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.create(owner, validRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INV-NEW");
    }

    @Test
    void create_throwsNotFound_whenClientNotFound() {
        when(invoiceRepository.existsByOwnerIdAndInvoiceNumber(any(), any())).thenReturn(false);
        when(clientRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.create(owner, validRequest))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_savesInvoice_withCalculatedTotals() {
        when(invoiceRepository.existsByOwnerIdAndInvoiceNumber(any(), any())).thenReturn(false);
        when(clientRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(client));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(99L);
            return i;
        });

        InvoiceResponse response = invoiceService.create(owner, validRequest);

        verify(invoiceRepository).save(argThat(inv ->
                inv.getTotal().compareTo(BigDecimal.TEN) == 0 &&
                inv.getItems().size() == 1
        ));
    }

    @Test
    void update_throwsBusiness_whenInvoiceIsPaid() {
        when(invoiceRepository.findByIdAndOwnerId(100L, 1L)).thenReturn(Optional.of(paidInvoice));

        assertThatThrownBy(() -> invoiceService.update(owner, 100L, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("paid invoice");
    }

    @Test
    void delete_throwsBusiness_whenInvoiceIsPaid() {
        when(invoiceRepository.findByIdAndOwnerId(100L, 1L)).thenReturn(Optional.of(paidInvoice));

        assertThatThrownBy(() -> invoiceService.delete(owner, 100L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void findById_throwsNotFound_whenNotOwner() {
        when(invoiceRepository.findByIdAndOwnerId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.findById(owner, 999L))
                .isInstanceOf(NotFoundException.class);
    }
}
