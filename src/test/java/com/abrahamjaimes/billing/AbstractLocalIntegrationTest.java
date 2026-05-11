package com.abrahamjaimes.billing;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test base that uses the local PostgreSQL (no Docker needed).
 * Requires: brew services start postgresql@17
 */
@ActiveProfiles("integration-local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractLocalIntegrationTest {

    @LocalServerPort
    protected int port;

    protected TestRestTemplate rest;

    @BeforeEach
    void buildRestTemplate() {
        // TestRestTemplate(RestTemplateBuilder) automatically adds NoOpResponseErrorHandler
        // and builds the RestTemplate from the builder — no 4xx/5xx exceptions thrown.
        // SimpleClientHttpRequestFactory avoids Apache HC auth-retry on 401 responses.
        var builder = new RestTemplateBuilder()
                .requestFactory(() -> new SimpleClientHttpRequestFactory())
                .rootUri("http://localhost:" + port);
        rest = new TestRestTemplate(builder);
    }
}
