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
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceLocalIntegrationTest extends AbstractLocalIntegrationTest {

    private HttpHeaders authHeaders;

    @BeforeEach
    void authenticate() {
        String unique = String.valueOf(System.nanoTime());
        var register = new RegisterRequest("inv-" + unique + "@test.com", "password123", "Inv", "User");
        AuthResponse auth = rest.postForEntity(
                "/api/v1/auth/register", register, AuthResponse.class).getBody();
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(auth.accessToken());
    }

    @Test
    void fullInvoiceFlow_createClientInvoiceAndPay() {
        // 1. Create client
        var clientReq = new ClientRequest("Integration Client", "ic@test.com", null, null, null, null);
        ClientResponse client = rest.exchange("/api/v1/clients", HttpMethod.POST,
                new HttpEntity<>(clientReq, authHeaders), ClientResponse.class).getBody();
        assertThat(client.id()).isPositive();

        // 2. Create invoice (SENT so we can pay it)
        String invoiceNum = "INV-LOCAL-" + System.nanoTime();
        var invoiceReq = new InvoiceRequest(
                client.id(), invoiceNum, InvoiceStatus.SENT,
                LocalDate.now(), LocalDate.now().plusDays(30),
                null, BigDecimal.ZERO,
                List.of(new InvoiceItemRequest("Consulting", BigDecimal.ONE, new BigDecimal("200.00")))
        );
        ResponseEntity<InvoiceResponse> invResp = rest.exchange("/api/v1/invoices", HttpMethod.POST,
                new HttpEntity<>(invoiceReq, authHeaders), InvoiceResponse.class);

        assertThat(invResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        InvoiceResponse invoice = invResp.getBody();
        assertThat(invoice.total()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.SENT);

        // 3. Partial payment — still SENT
        var partial = new PaymentRequest(new BigDecimal("80.00"), LocalDate.now(),
                PaymentMethod.BANK_TRANSFER, "TXN-A", null);
        ResponseEntity<PaymentResponse> payResp = rest.exchange(
                "/api/v1/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(partial, authHeaders), PaymentResponse.class);
        assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        InvoiceResponse afterPartial = rest.exchange("/api/v1/invoices/" + invoice.id(),
                HttpMethod.GET, new HttpEntity<>(authHeaders), InvoiceResponse.class).getBody();
        assertThat(afterPartial.status()).isEqualTo(InvoiceStatus.SENT);

        // 4. Final payment — automatically PAID
        var final_ = new PaymentRequest(new BigDecimal("120.00"), LocalDate.now(),
                PaymentMethod.CASH, null, null);
        rest.exchange("/api/v1/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(final_, authHeaders), PaymentResponse.class);

        InvoiceResponse afterFull = rest.exchange("/api/v1/invoices/" + invoice.id(),
                HttpMethod.GET, new HttpEntity<>(authHeaders), InvoiceResponse.class).getBody();
        assertThat(afterFull.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(afterFull.payments()).hasSize(2);
    }

    @Test
    void payment_onDraftInvoice_returns422() {
        var clientReq = new ClientRequest("Draft Client", null, null, null, null, null);
        ClientResponse client = rest.exchange("/api/v1/clients", HttpMethod.POST,
                new HttpEntity<>(clientReq, authHeaders), ClientResponse.class).getBody();

        String invoiceNum = "INV-DRAFT-" + System.nanoTime();
        var invoiceReq = new InvoiceRequest(client.id(), invoiceNum, InvoiceStatus.DRAFT,
                LocalDate.now(), LocalDate.now().plusDays(30), null, BigDecimal.ZERO,
                List.of(new InvoiceItemRequest("Item", BigDecimal.ONE, BigDecimal.TEN)));
        InvoiceResponse invoice = rest.exchange("/api/v1/invoices", HttpMethod.POST,
                new HttpEntity<>(invoiceReq, authHeaders), InvoiceResponse.class).getBody();

        var payReq = new PaymentRequest(BigDecimal.TEN, LocalDate.now(), PaymentMethod.CASH, null, null);
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(payReq, authHeaders), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void payment_exceedingBalance_returns422() {
        var clientReq = new ClientRequest("Overpay Client", null, null, null, null, null);
        ClientResponse client = rest.exchange("/api/v1/clients", HttpMethod.POST,
                new HttpEntity<>(clientReq, authHeaders), ClientResponse.class).getBody();

        String invoiceNum = "INV-OVER-" + System.nanoTime();
        var invoiceReq = new InvoiceRequest(client.id(), invoiceNum, InvoiceStatus.SENT,
                LocalDate.now(), LocalDate.now().plusDays(30), null, BigDecimal.ZERO,
                List.of(new InvoiceItemRequest("Item", BigDecimal.ONE, new BigDecimal("50.00"))));
        InvoiceResponse invoice = rest.exchange("/api/v1/invoices", HttpMethod.POST,
                new HttpEntity<>(invoiceReq, authHeaders), InvoiceResponse.class).getBody();

        var payReq = new PaymentRequest(new BigDecimal("999.00"), LocalDate.now(),
                PaymentMethod.CASH, null, null);
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(payReq, authHeaders), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
