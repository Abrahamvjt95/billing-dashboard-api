package com.abrahamjaimes.billing.config;

import com.abrahamjaimes.billing.entity.*;
import com.abrahamjaimes.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository       userRepository;
    private final ClientRepository     clientRepository;
    private final InvoiceRepository    invoiceRepository;
    private final PaymentRepository    paymentRepository;
    private final PasswordEncoder      passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail("demo@billflow.com")) {
            log.info("Demo data already present — skipping seed");
            return;
        }

        log.info("Seeding demo data...");

        // ── User ──────────────────────────────────────────────────────────
        User demo = userRepository.save(User.builder()
                .firstName("Demo")
                .lastName("User")
                .email("demo@billflow.com")
                .password(passwordEncoder.encode("demo1234"))
                .role(Role.USER)
                .enabled(true)
                .build());

        // ── Clients ───────────────────────────────────────────────────────
        Client acme = clientRepository.save(client(demo,
                "Acme Corp", "billing@acme.com", "+1 555 0100",
                "350 Fifth Avenue, New York, NY 10118", "EIN-12-3456789",
                "Primary client — SaaS subscription + consulting"));

        Client globex = clientRepository.save(client(demo,
                "Globex Industries", "ap@globex.com", "+1 555 0200",
                "742 Evergreen Terrace, Springfield, IL 62701", "EIN-98-7654321",
                "Annual retainer for infrastructure work"));

        Client initech = clientRepository.save(client(demo,
                "Initech LLC", "finance@initech.io", "+1 555 0300",
                "4120 Freidrich Lane, Austin, TX 78744", "EIN-55-1122334",
                null));

        Client umbrella = clientRepository.save(client(demo,
                "Umbrella Solutions", "payments@umbrella.co", "+44 20 7946 0958",
                "12 Broad Street, London, EC2M 1JH", "GB-VAT-123456789",
                "European client — invoices in USD"));

        Client soylent = clientRepository.save(client(demo,
                "Soylent Dynamics", "admin@soylent.dev", "+1 555 0500",
                "1 Infinite Loop, Cupertino, CA 95014", null,
                "Startup — net-30 terms"));

        // ── Invoices ──────────────────────────────────────────────────────

        // PAID — full payment
        Invoice inv001 = buildInvoice(demo, acme, "INV-2026-001",
                InvoiceStatus.PAID,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 14),
                new BigDecimal("8"), List.of(
                        item("Angular frontend development", 40, "150.00", 0),
                        item("Spring Boot API development",  30, "150.00", 1),
                        item("PostgreSQL schema design",     8,  "125.00", 2)
                ), "Phase 1 — MVP delivery");
        invoiceRepository.save(inv001);
        paymentRepository.save(payment(inv001, "9175.00",
                LocalDate.of(2026, 2, 10), PaymentMethod.BANK_TRANSFER, "TXN-ACM-001"));

        // PAID — two partial payments
        Invoice inv002 = buildInvoice(demo, globex, "INV-2026-002",
                InvoiceStatus.PAID,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 3),
                new BigDecimal("0"), List.of(
                        item("Monthly infrastructure retainer", 1, "3500.00", 0),
                        item("On-call support (10 h)",           10, "120.00", 1)
                ), null);
        invoiceRepository.save(inv002);
        paymentRepository.save(payment(inv002, "2000.00",
                LocalDate.of(2026, 2, 15), PaymentMethod.BANK_TRANSFER, "TXN-GLB-001"));
        paymentRepository.save(payment(inv002, "2700.00",
                LocalDate.of(2026, 3, 1),  PaymentMethod.BANK_TRANSFER, "TXN-GLB-002"));

        // SENT — partially paid
        Invoice inv003 = buildInvoice(demo, acme, "INV-2026-003",
                InvoiceStatus.SENT,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                new BigDecimal("16"), List.of(
                        item("UI component library",    60, "150.00", 0),
                        item("API integration layer",   20, "150.00", 1),
                        item("QA & automated tests",    15, "100.00", 2),
                        item("Technical documentation",  5,  "80.00", 3)
                ), "Phase 2 — Client portal");
        invoiceRepository.save(inv003);
        paymentRepository.save(payment(inv003, "5000.00",
                LocalDate.of(2026, 3, 20), PaymentMethod.CREDIT_CARD, "TXN-ACM-002"));

        // SENT — no payments yet
        Invoice inv004 = buildInvoice(demo, umbrella, "INV-2026-004",
                InvoiceStatus.SENT,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1),
                new BigDecimal("0"), List.of(
                        item("Backend API consulting",  20, "175.00", 0),
                        item("Code review & audit",      8, "175.00", 1)
                ), "April consulting services");
        invoiceRepository.save(inv004);

        // OVERDUE
        Invoice inv005 = buildInvoice(demo, initech, "INV-2026-005",
                InvoiceStatus.OVERDUE,
                LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 17),
                new BigDecimal("10"), List.of(
                        item("Legacy system migration",    35, "130.00", 0),
                        item("Data mapping & validation",  10, "100.00", 1)
                ), "Payment overdue — follow up required");
        invoiceRepository.save(inv005);

        // OVERDUE — partial payment
        Invoice inv006 = buildInvoice(demo, soylent, "INV-2026-006",
                InvoiceStatus.OVERDUE,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 9),
                new BigDecimal("0"), List.of(
                        item("MVP development sprint 1", 1, "4800.00", 0),
                        item("DevOps setup & CI/CD",      1, "1200.00", 1)
                ), null);
        invoiceRepository.save(inv006);
        paymentRepository.save(payment(inv006, "3000.00",
                LocalDate.of(2026, 4, 5), PaymentMethod.CASH, "CASH-SOY-001"));

        // DRAFT
        Invoice inv007 = buildInvoice(demo, globex, "INV-2026-007",
                InvoiceStatus.DRAFT,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                new BigDecimal("0"), List.of(
                        item("Monthly infrastructure retainer", 1, "3500.00", 0),
                        item("Performance optimization",        12, "130.00", 1),
                        item("On-call support (5 h)",            5, "120.00", 2)
                ), "May retainer — pending review");
        invoiceRepository.save(inv007);

        // DRAFT
        Invoice inv008 = buildInvoice(demo, umbrella, "INV-2026-008",
                InvoiceStatus.DRAFT,
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 6, 4),
                new BigDecimal("0"), List.of(
                        item("API gateway design",    16, "175.00", 0),
                        item("Security audit",         8, "200.00", 1)
                ), null);
        invoiceRepository.save(inv008);

        log.info("Demo data seeded: 1 user, 5 clients, 8 invoices");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Client client(User owner, String name, String email,
                          String phone, String address, String taxId, String notes) {
        return Client.builder()
                .owner(owner).name(name).email(email)
                .phone(phone).address(address).taxId(taxId).notes(notes)
                .build();
    }

    private Invoice buildInvoice(User owner, Client client, String number,
                                 InvoiceStatus status,
                                 LocalDate issueDate, LocalDate dueDate,
                                 BigDecimal taxRate, List<InvoiceItem> items,
                                 String notes) {
        Invoice inv = Invoice.builder()
                .owner(owner).client(client)
                .invoiceNumber(number).status(status)
                .issueDate(issueDate).dueDate(dueDate)
                .taxRate(taxRate).notes(notes)
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();

        for (InvoiceItem it : items) {
            it.setInvoice(inv);
            inv.getItems().add(it);
        }
        inv.recalculateTotals();
        return inv;
    }

    private InvoiceItem item(String desc, double qty, String unitPrice, int order) {
        InvoiceItem it = InvoiceItem.builder()
                .description(desc)
                .quantity(new BigDecimal(qty))
                .unitPrice(new BigDecimal(unitPrice))
                .amount(BigDecimal.ZERO)
                .sortOrder(order)
                .build();
        it.calculateAmount();
        return it;
    }

    private Payment payment(Invoice invoice, String amount,
                            LocalDate date, PaymentMethod method, String ref) {
        return Payment.builder()
                .invoice(invoice)
                .amount(new BigDecimal(amount))
                .paymentDate(date)
                .method(method)
                .reference(ref)
                .build();
    }
}
