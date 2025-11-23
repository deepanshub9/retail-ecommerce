/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.ui.services.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.amazon.sample.ui.client.catalog.CatalogClient;
import com.amazon.sample.ui.client.catalog.models.model.Product;
import com.amazon.sample.ui.client.catalog.models.model.Tag;
import com.amazon.sample.ui.services.catalog.model.ProductPage;
import com.amazon.sample.ui.services.catalog.model.ProductTag;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catalog Service Tests")
class CatalogServiceTest {

    @Mock
    private CatalogClient catalogClient;

    private KiotaCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new KiotaCatalogService(catalogClient);
    }

    @Test
    @DisplayName("Should get products successfully")
    void shouldGetProductsSuccessfully() {
        // Given
        List<Product> mockProducts = Arrays.asList(
            createMockProduct("1", "Product 1", 2999),
            createMockProduct("2", "Product 2", 3999)
        );

        // Mock the client response
        when(catalogClient.catalog().products().get(any()))
            .thenReturn(mockProducts);

        // When
        Mono<ProductPage> result = catalogService.getProducts("", "", 1, 6);

        // Then
        StepVerifier.create(result)
            .assertNext(productPage -> {
                assertNotNull(productPage);
                assertEquals(2, productPage.getProducts().size());
                assertEquals("Product 1", productPage.getProducts().get(0).getName());
                assertEquals("Product 2", productPage.getProducts().get(1).getName());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should get single product successfully")
    void shouldGetSingleProductSuccessfully() {
        // Given
        Product mockProduct = createMockProduct("123", "Test Product", 4999);
        mockProduct.setDescription("Test Description");

        when(catalogClient.catalog().products().byId("123").get())
            .thenReturn(mockProduct);

        // When
        Mono<com.amazon.sample.ui.services.catalog.model.Product> result = 
            catalogService.getProduct("123");

        // Then
        StepVerifier.create(result)
            .assertNext(product -> {
                assertNotNull(product);
                assertEquals("123", product.getId());
                assertEquals("Test Product", product.getName());
                assertEquals(4999, product.getPrice());
                assertEquals("Test Description", product.getDescription());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should get tags successfully")
    void shouldGetTagsSuccessfully() {
        // Given
        List<Tag> mockTags = Arrays.asList(
            createMockTag("electronics", "Electronics"),
            createMockTag("clothing", "Clothing")
        );

        when(catalogClient.catalog().tags().get())
            .thenReturn(mockTags);

        // When
        Flux<ProductTag> result = catalogService.getTags();

        // Then
        StepVerifier.create(result)
            .assertNext(tag -> {
                assertEquals("electronics", tag.getName());
                assertEquals("Electronics", tag.getDisplayName());
            })
            .assertNext(tag -> {
                assertEquals("clothing", tag.getName());
                assertEquals("Clothing", tag.getDisplayName());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty product list")
    void shouldHandleEmptyProductList() {
        // Given
        when(catalogClient.catalog().products().get(any()))
            .thenReturn(Arrays.asList());

        // When
        Mono<ProductPage> result = catalogService.getProducts("", "", 1, 6);

        // Then
        StepVerifier.create(result)
            .assertNext(productPage -> {
                assertNotNull(productPage);
                assertTrue(productPage.getProducts().isEmpty());
                assertEquals(0, productPage.getTotal());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle client errors gracefully")
    void shouldHandleClientErrorsGracefully() {
        // Given
        when(catalogClient.catalog().products().get(any()))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When
        Mono<ProductPage> result = catalogService.getProducts("", "", 1, 6);

        // Then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    @DisplayName("Should handle product not found")
    void shouldHandleProductNotFound() {
        // Given
        when(catalogClient.catalog().products().byId("nonexistent").get())
            .thenThrow(new RuntimeException("Product not found"));

        // When
        Mono<com.amazon.sample.ui.services.catalog.model.Product> result = 
            catalogService.getProduct("nonexistent");

        // Then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    @DisplayName("Should filter products by tag")
    void shouldFilterProductsByTag() {
        // Given
        List<Product> mockProducts = Arrays.asList(
            createMockProduct("1", "Electronics Product", 9999)
        );

        when(catalogClient.catalog().products().get(any()))
            .thenReturn(mockProducts);

        // When
        Mono<ProductPage> result = catalogService.getProducts("electronics", "", 1, 6);

        // Then
        StepVerifier.create(result)
            .assertNext(productPage -> {
                assertNotNull(productPage);
                assertEquals(1, productPage.getProducts().size());
                assertEquals("Electronics Product", productPage.getProducts().get(0).getName());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle pagination parameters")
    void shouldHandlePaginationParameters() {
        // Given
        List<Product> mockProducts = Arrays.asList(
            createMockProduct("7", "Product 7", 1999),
            createMockProduct("8", "Product 8", 2499),
            createMockProduct("9", "Product 9", 2999)
        );

        when(catalogClient.catalog().products().get(any()))
            .thenReturn(mockProducts);

        // When
        Mono<ProductPage> result = catalogService.getProducts("", "", 2, 3);

        // Then
        StepVerifier.create(result)
            .assertNext(productPage -> {
                assertNotNull(productPage);
                assertEquals(3, productPage.getProducts().size());
                assertEquals(2, productPage.getPage());
                assertEquals(3, productPage.getSize());
            })
            .verifyComplete();
    }

    private Product createMockProduct(String id, String name, int price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        return product;
    }

    private Tag createMockTag(String name, String displayName) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDisplayName(displayName);
        return tag;
    }
}