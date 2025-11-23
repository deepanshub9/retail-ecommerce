/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.ui.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.amazon.sample.ui.services.catalog.CatalogService;
import com.amazon.sample.ui.services.catalog.model.Product;
import com.amazon.sample.ui.services.catalog.model.ProductPage;
import com.amazon.sample.ui.services.catalog.model.ProductTag;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(CatalogController.class)
@DisplayName("Catalog Controller Tests")
class CatalogControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CatalogService catalogService;

    @Test
    @DisplayName("Should display catalog page with products")
    void shouldDisplayCatalogPageWithProducts() {
        // Given
        List<Product> products = Arrays.asList(
            createProduct("1", "Product 1", 29.99),
            createProduct("2", "Product 2", 39.99)
        );
        
        ProductPage productPage = new ProductPage();
        productPage.setProducts(products);
        productPage.setPage(1);
        productPage.setSize(6);
        productPage.setTotal(2);

        List<ProductTag> tags = Arrays.asList(
            createTag("electronics", "Electronics"),
            createTag("clothing", "Clothing")
        );

        when(catalogService.getProducts(anyString(), anyString(), anyInt(), anyInt()))
            .thenReturn(Mono.just(productPage));
        when(catalogService.getTags()).thenReturn(Flux.fromIterable(tags));

        // When & Then
        webTestClient.get()
            .uri("/catalog")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assert body.contains("Product 1");
                assert body.contains("Product 2");
            });
    }

    @Test
    @DisplayName("Should display product detail page")
    void shouldDisplayProductDetailPage() {
        // Given
        Product product = createProduct("123", "Detailed Product", 149.99);
        product.setDescription("This is a detailed product description");

        List<Product> recommendations = Arrays.asList(
            createProduct("456", "Recommended Product 1", 79.99)
        );

        ProductPage recommendationsPage = new ProductPage();
        recommendationsPage.setProducts(recommendations);

        when(catalogService.getProduct("123")).thenReturn(Mono.just(product));
        when(catalogService.getProducts("", "", 1, 6))
            .thenReturn(Mono.just(recommendationsPage));

        // When & Then
        webTestClient.get()
            .uri("/catalog/123")
            .exchange()
            .expectStatus().isOk();
    }

    private Product createProduct(String id, String name, double price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice((int) (price * 100));
        return product;
    }

    private ProductTag createTag(String name, String displayName) {
        ProductTag tag = new ProductTag();
        tag.setName(name);
        tag.setDisplayName(displayName);
        return tag;
    }
}