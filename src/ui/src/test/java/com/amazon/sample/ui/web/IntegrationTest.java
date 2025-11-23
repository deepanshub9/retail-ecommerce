/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.ui.web;

import static org.hamcrest.Matchers.containsString;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("UI Integration Tests")
class IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static MockWebServer mockCatalogService;
    private static MockWebServer mockCartService;
    private static MockWebServer mockCheckoutService;

    @BeforeEach
    void setUp() throws Exception {
        mockCatalogService = new MockWebServer();
        mockCartService = new MockWebServer();
        mockCheckoutService = new MockWebServer();
        
        mockCatalogService.start();
        mockCartService.start();
        mockCheckoutService.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockCatalogService.shutdown();
        mockCartService.shutdown();
        mockCheckoutService.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mockCatalogService != null) {
            registry.add("endpoints.catalog", 
                () -> "http://localhost:" + mockCatalogService.getPort());
        }
        if (mockCartService != null) {
            registry.add("endpoints.cart", 
                () -> "http://localhost:" + mockCartService.getPort());
        }
        if (mockCheckoutService != null) {
            registry.add("endpoints.checkout", 
                () -> "http://localhost:" + mockCheckoutService.getPort());
        }
    }

    @Test
    @DisplayName("Should load home page successfully")
    void shouldLoadHomePageSuccessfully() {
        webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(containsString("html"));
    }

    @Test
    @DisplayName("Should load catalog page with mocked service")
    void shouldLoadCatalogPageWithMockedService() {
        // Given
        String mockProductsResponse = """
            [
                {
                    "id": "1",
                    "name": "Test Product",
                    "price": 2999,
                    "imageUrl": "/assets/img/products/1.jpg"
                }
            ]
            """;

        String mockTagsResponse = """
            [
                {
                    "name": "electronics",
                    "displayName": "Electronics"
                }
            ]
            """;

        mockCatalogService.enqueue(new MockResponse()
            .setBody(mockProductsResponse)
            .addHeader("Content-Type", "application/json"));

        mockCatalogService.enqueue(new MockResponse()
            .setBody(mockTagsResponse)
            .addHeader("Content-Type", "application/json"));

        // When & Then
        webTestClient.get()
            .uri("/catalog")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(containsString("Test Product"));
    }

    @Test
    @DisplayName("Should handle service unavailable gracefully")
    void shouldHandleServiceUnavailableGracefully() {
        // Given
        mockCatalogService.enqueue(new MockResponse().setResponseCode(503));
        mockCatalogService.enqueue(new MockResponse().setResponseCode(503));

        // When & Then
        webTestClient.get()
            .uri("/catalog")
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should load cart page")
    void shouldLoadCartPage() {
        // Given
        String mockCartResponse = """
            {
                "items": [],
                "subtotal": 0
            }
            """;

        mockCartService.enqueue(new MockResponse()
            .setBody(mockCartResponse)
            .addHeader("Content-Type", "application/json"));

        // When & Then
        webTestClient.get()
            .uri("/cart")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Should handle health check endpoint")
    void shouldHandleHealthCheckEndpoint() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(containsString("status"));
    }

    @Test
    @DisplayName("Should handle metrics endpoint")
    void shouldHandleMetricsEndpoint() {
        webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Should handle topology endpoint")
    void shouldHandleTopologyEndpoint() {
        webTestClient.get()
            .uri("/topology")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(containsString("html"));
    }

    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() {
        // Given
        for (int i = 0; i < 10; i++) {
            mockCatalogService.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));
            mockCatalogService.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));
        }

        // When & Then - Make multiple concurrent requests
        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                .uri("/catalog")
                .exchange()
                .expectStatus().isOk();
        }
    }

    @Test
    @DisplayName("Should handle static assets")
    void shouldHandleStaticAssets() {
        webTestClient.get()
            .uri("/assets/css/styles.css")
            .exchange()
            .expectStatus().isOk();
    }
}