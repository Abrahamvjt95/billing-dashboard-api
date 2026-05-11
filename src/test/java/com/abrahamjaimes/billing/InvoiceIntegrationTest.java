package com.abrahamjaimes.billing;

import com.abrahamjaimes.billing.dto.request.*;
import com.abrahamjaimes.billing.dto.response.AuthResponse;
import com.abrahamjaimes.billing.dto.response.ClientResponse;
import com.abrahamjaimes.billing.dto.response.InvoiceResponse;
import com.abrahamjaimes.billing.dto.response.PaymentResponse;
import com.abrahamjaimes.billing.entity.InvoiceStatus;
import com.abrahamjaimes.billing.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;

    private HttpHeaders authHeaders;

    @BeforeEach
    void authenticate() {
        var register = new RegisterRequest(
                "invoice-user-" + System.nanoTime() + "@test.com",
                "password123", "Invoice", "User");
        AuthResponse auth = rest.postForEntity(
                "/api/v1/auth/register", register, AuthResponse.class).getBody();
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(auth.accessToken());
    }

    @Test
    void fullInvoiceFlow_createClientInvoiceAndPay() {
        // 1. Create client
        var clientReq = new ClientRequest("Test Client", "client@test.com", null, null, null, null);
        ClientResponse client = rest.exchange(
                "/api/v1/clients", HttpMethod.POST,
                new HttpEntity<>(clientReq, authHeaders), ClientResponse.class).getBody();
        assertThat(client.id()).isPositive();

        // 2. Create invoice
        var invoiceReq = new InvoiceRequest(
                client.id(), "INV-IT-001", InvoiceStatus.SENT,
                LocalDate.now(), LocalDate.now().plusDays(30),
                null, BigDecimal.ZERO,
                List.of(new InvoiceItemRequest("Dev work", BigDecimal.ONE, new BigDecimal("100.00")))
        );
        ResponseEntity<InvoiceResponse> invResp = rest.exchange(
                "/api/v1/invoices", HttpMethod.POST,
                new HttpEntity<>(invoiceReq, authHeaders), InvoiceResponse.class);

        assertThat(invResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        InvoiceResponse invoice = invResp.getBody();
        assertThat(invoice.total()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.SENT);

        // 3. Partial payment
        var payReq = new PaymentRequest(
                new BigDecimal("60.00"), LocalDate.now(), PaymentMethod.BANK_TRANSFER, "TXN-1", null);
        ResponseEntity<PaymentResponse> payResp = rest.exchange(
                "/api/v1/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(payReq, authHeaders), PaymentResponse.class);

        assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(payResp.getBody().amount()).isEqualByComparingTo(new BigDecimal("60.00"));

        // Verify still SENT
        InvoiceResponse after1st = rest.exchange(
                "/api/v1/invoices/" + invoice.id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders), InvoiceResponse.class).getBody();
        assertThat(after1st.status()).isEqualTo(InvoiceStatus.SENT);

        // 4. Final payment — fully paid
        var finalPay = new PaymentRequest(
                new BigDecimal("40.00"), LocalDate.now(), PaymentMethod.CASH, null, null);
        rest.exchange("/api/v1/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(finalPay, authHeaders), PaymentResponse.class);

        InvoiceResponse afterFull = rest.exchange(
                "/api/v1/invoices/" + invoice.id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders), InvoiceResponse.class).getBody();
        assertThat(afterFull.status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void payment_onDraftInvoice_returns422() {
        var clientReq = new ClientRequest("Draft Client", null, null, null, null, null);
        ClientResponse client = rest.exchange(
                "/api/v1/clients", HttpMethod.POST,
                new HttpEntity<>(clientReq, authHeaders), ClientResponse.class).getBody();

        var invoiceReq = new InvoiceRequest(
                client.id(), "INV-DRAFT-01", InvoiceStatus.DRAFT,
                LocalDate.now(), LocalDate.now().plusDays(30),
                null, BigDecimal.ZERO,
                List.of(new InvoiceItemRequest("Item", BigDecimal.ONE, BigDecimal.TEN))
        );
        InvoiceResponse invoice = rest.exchange(
                "/api/v1/invoices", HttpMethod.POST,
                new HttpEntity<>(invoiceReq, authHeaders), InvoiceResponse.class).getBody();

        var payReq = new PaymentRequest(
                BigDecimal.TEN, LocalDate.now(), PaymentMethod.CASH, null, null);
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(payReq, authHeaders), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
