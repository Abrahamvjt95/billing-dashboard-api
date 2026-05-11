package com.abrahamjaimes.billing;

import com.abrahamjaimes.billing.dto.request.LoginRequest;
import com.abrahamjaimes.billing.dto.request.RegisterRequest;
import com.abrahamjaimes.billing.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;

    @Test
    void register_thenLogin_returnsValidTokens() {
        var register = new RegisterRequest("integration@test.com", "password123", "Test", "User");
        ResponseEntity<AuthResponse> regResp = rest.postForEntity(
                "/api/v1/auth/register", register, AuthResponse.class);

        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(regResp.getBody()).isNotNull();
        assertThat(regResp.getBody().accessToken()).isNotBlank();
        assertThat(regResp.getBody().refreshToken()).isNotBlank();
        assertThat(regResp.getBody().user().email()).isEqualTo("integration@test.com");

        var login = new LoginRequest("integration@test.com", "password123");
        ResponseEntity<AuthResponse> loginResp = rest.postForEntity(
                "/api/v1/auth/login", login, AuthResponse.class);

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody().accessToken()).isNotBlank();
    }

    @Test
    void register_duplicateEmail_returns409() {
        var request = new RegisterRequest("duplicate@test.com", "password123", "A", "B");
        rest.postForEntity("/api/v1/auth/register", request, AuthResponse.class);

        ResponseEntity<String> second = rest.postForEntity(
                "/api/v1/auth/register", request, String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_wrongPassword_returns401() {
        var register = new RegisterRequest("wrongpwd@test.com", "password123", "A", "B");
        rest.postForEntity("/api/v1/auth/register", register, AuthResponse.class);

        var login = new LoginRequest("wrongpwd@test.com", "wrong-password");
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", login, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401or403() {
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/clients", String.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() {
        var register = new RegisterRequest("auth-test@test.com", "password123", "A", "B");
        AuthResponse auth = rest.postForEntity(
                "/api/v1/auth/register", register, AuthResponse.class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.accessToken());
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/clients", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
