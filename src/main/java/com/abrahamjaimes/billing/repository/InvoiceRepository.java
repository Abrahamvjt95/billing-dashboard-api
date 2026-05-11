package com.abrahamjaimes.billing.repository;

import com.abrahamjaimes.billing.entity.Invoice;
import com.abrahamjaimes.billing.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Page<Invoice> findAllByOwnerId(Long ownerId, Pageable pageable);
    Page<Invoice> findAllByOwnerIdAndStatus(Long ownerId, InvoiceStatus status, Pageable pageable);
    Optional<Invoice> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerIdAndStatus(Long ownerId, InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.owner.id = :ownerId AND i.status = :status")
    BigDecimal sumTotalByOwnerIdAndStatus(Long ownerId, InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.owner.id = :ownerId")
    BigDecimal sumTotalByOwnerId(Long ownerId);

    List<Invoice> findByOwnerIdAndStatusIn(Long ownerId, List<InvoiceStatus> statuses);

    boolean existsByOwnerIdAndInvoiceNumber(Long ownerId, String invoiceNumber);
}
