package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.response.DashboardStatsResponse;
import com.abrahamjaimes.billing.entity.InvoiceStatus;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.repository.ClientRepository;
import com.abrahamjaimes.billing.repository.InvoiceRepository;
import com.abrahamjaimes.billing.repository.PaymentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public DashboardService(ClientRepository clientRepository, InvoiceRepository invoiceRepository,
                            PaymentRepository paymentRepository) {
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public DashboardStatsResponse getStats(User owner) {
        Long ownerId = owner.getId();

        long totalClients = clientRepository.findAllByOwnerId(ownerId, Pageable.unpaged()).getTotalElements();
        long totalInvoices = invoiceRepository.findAllByOwnerId(ownerId, Pageable.unpaged()).getTotalElements();
        long draftInvoices = invoiceRepository.countByOwnerIdAndStatus(ownerId, InvoiceStatus.DRAFT);
        long sentInvoices = invoiceRepository.countByOwnerIdAndStatus(ownerId, InvoiceStatus.SENT);
        long paidInvoices = invoiceRepository.countByOwnerIdAndStatus(ownerId, InvoiceStatus.PAID);
        long overdueInvoices = invoiceRepository.countByOwnerIdAndStatus(ownerId, InvoiceStatus.OVERDUE);

        BigDecimal totalRevenue = invoiceRepository.sumTotalByOwnerId(ownerId);
        BigDecimal collectedRevenue = paymentRepository.sumAmountByOwnerId(ownerId);
        BigDecimal pendingRevenue = totalRevenue.subtract(collectedRevenue);

        return new DashboardStatsResponse(totalClients, totalInvoices, draftInvoices, sentInvoices,
                paidInvoices, overdueInvoices, totalRevenue, pendingRevenue, collectedRevenue);
    }
}
