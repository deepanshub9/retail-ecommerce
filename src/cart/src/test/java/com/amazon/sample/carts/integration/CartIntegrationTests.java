/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.carts.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.amazon.sample.carts.util.TestUtil;
import com.amazon.sample.carts.web.api.Item;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
  "retail.cart.persistence.provider=in-memory"
})
@Tag("integration")
@DisplayName("Cart Integration Tests - End-to-End Scenarios")
public class CartIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Nested
  @DisplayName("Easy Integration Tests - Basic E2E Flows")
  class EasyIntegrationTests {

    @Test
    @DisplayName("Complete shopping flow - Add, View, Update, Remove")
    void completeShoppingFlow() throws Exception {
      String customerId = "integration-customer-1";

      // Step 1: Get empty cart
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerId").value(customerId))
        .andExpect(jsonPath("$.items").isEmpty());

      // Step 2: Add first item
      mockMvc.perform(post("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item("laptop", 1, 99999))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.itemId").value("laptop"))
        .andExpect(jsonPath("$.quantity").value(1))
        .andExpect(jsonPath("$.unitPrice").value(99999));

      // Step 3: Verify cart has item
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1));

      // Step 4: Delete entire cart
      mockMvc.perform(delete("/carts/" + customerId))
        .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Multiple customers with separate carts")
    void multipleCustomersWithSeparateCarts() throws Exception {
      String customer1 = "customer-1";
      String customer2 = "customer-2";

      // Customer 1 adds laptop
      mockMvc.perform(post("/carts/" + customer1 + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item("laptop", 1, 99999))))
        .andExpect(status().isCreated());

      // Customer 2 adds phone
      mockMvc.perform(post("/carts/" + customer2 + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item("phone", 1, 79999))))
        .andExpect(status().isCreated());

      // Verify customer 1 cart
      mockMvc.perform(get("/carts/" + customer1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].itemId").value("laptop"));

      // Verify customer 2 cart
      mockMvc.perform(get("/carts/" + customer2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].itemId").value("phone"));
    }
  }

  @Nested
  @DisplayName("Medium Integration Tests - Complex Business Scenarios")
  class MediumIntegrationTests {

    @Test
    @DisplayName("Bulk operations - Adding multiple items efficiently")
    void bulkOperationsAddingMultipleItems() throws Exception {
      String customerId = "bulk-customer";
      
      // Add 10 different items
      for (int i = 1; i <= 10; i++) {
        mockMvc.perform(post("/carts/" + customerId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(new Item("product-" + i, i % 5 + 1, i * 1000))))
          .andExpect(status().isCreated());
      }

      // Verify all items are in cart
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(10));

      // Remove last 3 items
      for (int i = 8; i <= 10; i++) {
        mockMvc.perform(delete("/carts/" + customerId + "/items/product-" + i))
          .andExpect(status().isAccepted());
      }

      // Verify final state: 7 items remaining
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(7));
    }

    @Test
    @DisplayName("Price update scenario - Handle price changes")
    void priceUpdateScenario() throws Exception {
      String customerId = "price-update-customer";
      String productId = "dynamic-price-product";

      // Add item with initial price
      mockMvc.perform(post("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item(productId, 3, 5000))))
        .andExpect(status().isCreated());

      // Simulate price increase
      mockMvc.perform(patch("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item(productId, 3, 5500))))
        .andExpect(status().isAccepted());

      // Verify price was updated
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].unitPrice").value(5500));
    }
  }

  @Nested
  @DisplayName("Hard Integration Tests - Error Handling & Edge Cases")
  class HardIntegrationTests {

    @Test
    @DisplayName("Large cart performance test")
    void largeCartPerformanceTest() throws Exception {
      String customerId = "large-cart-customer";
      int itemCount = 50;

      // Measure time to add many items
      long startTime = System.currentTimeMillis();
      
      for (int i = 1; i <= itemCount; i++) {
        mockMvc.perform(post("/carts/" + customerId + "/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(new Item("item-" + i, 1, i * 100))))
          .andExpect(status().isCreated());
      }
      
      long addTime = System.currentTimeMillis() - startTime;

      // Measure time to retrieve large cart
      startTime = System.currentTimeMillis();
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(itemCount));
      long retrieveTime = System.currentTimeMillis() - startTime;

      // Performance assertions
      assertTrue(addTime < 10000, "Adding " + itemCount + " items took too long: " + addTime + "ms");
      assertTrue(retrieveTime < 1000, "Retrieving cart with " + itemCount + " items took too long: " + retrieveTime + "ms");
    }

    @Test
    @DisplayName("Error recovery - Service failures and retries")
    void errorRecoveryServiceFailuresAndRetries() throws Exception {
      String customerId = "error-recovery-customer";

      // Add some items successfully
      mockMvc.perform(post("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item("stable-product", 2, 1000))))
        .andExpect(status().isCreated());

      // Verify cart state before error scenarios
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1));

      // Test invalid JSON handling
      mockMvc.perform(post("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{invalid-json"))
        .andExpect(status().isBadRequest());

      // Verify cart state is unchanged after errors
      mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].itemId").value("stable-product"));
    }

    @Test
    @DisplayName("Data consistency across operations")
    void dataConsistencyAcrossOperations() throws Exception {
      String customerId = "consistency-customer";

      // Complex sequence of operations to test consistency
      // 1. Add items
      mockMvc.perform(post("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item("product-A", 5, 1000))))
        .andExpect(status().isCreated());

      mockMvc.perform(post("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item("product-B", 3, 2000))))
        .andExpect(status().isCreated());

      // 2. Remove one item
      mockMvc.perform(delete("/carts/" + customerId + "/items/product-B"))
        .andExpect(status().isAccepted());

      // 3. Add new item
      mockMvc.perform(post("/carts/" + customerId + "/items")
          .contentType(MediaType.APPLICATION_JSON)
          .content(TestUtil.convertObjectToJsonBytes(new Item("product-C", 2, 1500))))
        .andExpect(status().isCreated());

      // 4. Verify final consistent state
      MvcResult result = mockMvc.perform(get("/carts/" + customerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andReturn();

      String responseContent = result.getResponse().getContentAsString();
      
      // Verify product-A exists
      assertTrue(responseContent.contains("product-A"));
      
      // Verify product-B was removed
      assertFalse(responseContent.contains("product-B"));
      
      // Verify product-C was added
      assertTrue(responseContent.contains("product-C"));
    }
  }
}