package com.abrahamjaimes.billing;

import com.abrahamjaimes.billing.dto.request.LoginRequest;
import com.abrahamjaimes.billing.dto.request.RegisterRequest;
import com.abrahamjaimes.billing.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Test
    void register_thenLogin_returnsValidTokens() {
        String unique = String.valueOf(System.nanoTime());
        var register = new RegisterRequest("local-" + unique + "@test.com", "password123", "Local", "User");
        ResponseEntity<AuthResponse> regResp = rest.postForEntity(
                "/api/v1/auth/register", register, AuthResponse.class);

        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(regResp.getBody().accessToken()).isNotBlank();
        assertThat(regResp.getBody().refreshToken()).isNotBlank();

        var login = new LoginRequest("local-" + unique + "@test.com", "password123");
        ResponseEntity<AuthResponse> loginResp = rest.postForEntity(
                "/api/v1/auth/login", login, AuthResponse.class);

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody().accessToken()).isNotBlank();
    }

    @Test
    void register_duplicateEmail_returns409() {
        String unique = String.valueOf(System.nanoTime());
        var request = new RegisterRequest("dup-" + unique + "@test.com", "password123", "A", "B");
        rest.postForEntity("/api/v1/auth/register", request, AuthResponse.class);

        ResponseEntity<String> second = rest.postForEntity(
                "/api/v1/auth/register", request, String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String unique = String.valueOf(System.nanoTime());
        rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest("wp-" + unique + "@test.com", "password123", "A", "B"),
                AuthResponse.class);

        // JDK 11+ HttpClient handles 401 cleanly without throwing — no streaming-mode retry issues.
        String body = "{\"email\":\"wp-" + unique + "@test.com\",\"password\":\"wrong\"}";
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401or403() {
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/clients", String.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() {
        String unique = String.valueOf(System.nanoTime());
        var register = new RegisterRequest("prot-" + unique + "@test.com", "password123", "A", "B");
        AuthResponse auth = rest.postForEntity(
                "/api/v1/auth/register", register, AuthResponse.class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.accessToken());
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/clients", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
