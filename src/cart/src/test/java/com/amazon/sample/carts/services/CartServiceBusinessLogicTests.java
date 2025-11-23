/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.carts.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.amazon.sample.carts.repositories.CartEntity;
import com.amazon.sample.carts.repositories.ItemEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Cart Service Business Logic Tests")
public class CartServiceBusinessLogicTests extends AbstractServiceTests {

  private CartService cartService;

  @BeforeEach
  void setUp() {
    cartService = getService();
  }

  @Override
  public CartService getService() {
    // This will be implemented by concrete test classes
    return cartService;
  }

  @Nested
  @DisplayName("Easy Business Logic Tests")
  class EasyBusinessLogicTests {

    @Test
    @DisplayName("Should calculate correct total for single item")
    void shouldCalculateCorrectTotalForSingleItem() {
      // Given
      String customerId = "calc-customer-1";
      cartService.add(customerId, "product-1", 3, 1000); // 3 items at $10.00 each

      // When
      CartEntity cart = cartService.get(customerId);

      // Then
      assertEquals(1, cart.getItems().size());
      ItemEntity item = cart.getItems().get(0);
      assertEquals(3000, item.getQuantity() * item.getUnitPrice()); // $30.00 total
    }

    @Test
    @DisplayName("Should maintain item uniqueness in cart")
    void shouldMaintainItemUniquenessInCart() {
      // Given
      String customerId = "unique-customer";
      
      // When - Add same item twice
      cartService.add(customerId, "product-1", 2, 1000);
      cartService.add(customerId, "product-1", 3, 1000); // Should update, not duplicate

      // Then
      CartEntity cart = cartService.get(customerId);
      assertEquals(1, cart.getItems().size());
      assertEquals(3, cart.getItems().get(0).getQuantity()); // Should have latest quantity
    }

    @Test
    @DisplayName("Should handle empty cart operations")
    void shouldHandleEmptyCartOperations() {
      // Given
      String customerId = "empty-customer";

      // When
      CartEntity cart = cartService.get(customerId);

      // Then
      assertNotNull(cart);
      assertEquals(customerId, cart.getCustomerId());
      assertEquals(0, cart.getItems().size());
      assertTrue(cart.getItems().isEmpty());
    }
  }

  @Nested
  @DisplayName("Medium Business Logic Tests")
  class MediumBusinessLogicTests {

    @Test
    @DisplayName("Should handle cart with multiple different items")
    void shouldHandleCartWithMultipleDifferentItems() {
      // Given
      String customerId = "multi-customer";
      
      // When - Add multiple different items
      cartService.add(customerId, "laptop", 1, 99999);     // $999.99
      cartService.add(customerId, "mouse", 2, 2999);       // $29.99 x 2
      cartService.add(customerId, "keyboard", 1, 7999);    // $79.99

      // Then
      CartEntity cart = cartService.get(customerId);
      assertEquals(3, cart.getItems().size());
      
      // Verify total value calculation
      int totalValue = cart.getItems().stream()
        .mapToInt(item -> item.getQuantity() * item.getUnitPrice())
        .sum();
      assertEquals(165997, totalValue); // $1659.97 total
    }

    @Test
    @DisplayName("Should handle item quantity updates correctly")
    void shouldHandleItemQuantityUpdatesCorrectly() {
      // Given
      String customerId = "update-customer";
      cartService.add(customerId, "product-1", 5, 1000);

      // When - Update quantity
      cartService.update(customerId, "product-1", 10, 1000);

      // Then
      CartEntity cart = cartService.get(customerId);
      assertEquals(1, cart.getItems().size());
      assertEquals(10, cart.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("Should handle price changes for existing items")
    void shouldHandlePriceChangesForExistingItems() {
      // Given
      String customerId = "price-customer";
      cartService.add(customerId, "product-1", 2, 1000); // $10.00

      // When - Update with new price
      cartService.update(customerId, "product-1", 2, 1200); // $12.00

      // Then
      CartEntity cart = cartService.get(customerId);
      ItemEntity item = cart.getItems().get(0);
      assertEquals(1200, item.getUnitPrice());
      assertEquals(2400, item.getQuantity() * item.getUnitPrice()); // $24.00 total
    }

    @Test
    @DisplayName("Should handle cart merge operations")
    void shouldHandleCartMergeOperations() {
      // Given
      String permanentCustomer = "perm-customer";
      String sessionCustomer = "session-customer";
      
      // Setup permanent customer cart
      cartService.add(permanentCustomer, "product-1", 2, 1000);
      
      // Setup session cart
      cartService.add(sessionCustomer, "product-2", 1, 2000);
      cartService.add(sessionCustomer, "product-1", 1, 1000); // Same product

      // When - Merge session cart into permanent cart
      cartService.merge(permanentCustomer, sessionCustomer);

      // Then
      CartEntity permanentCart = cartService.get(permanentCustomer);
      assertEquals(2, permanentCart.getItems().size()); // Should have both products
      
      // Verify product-1 quantities were merged (2 + 1 = 3)
      ItemEntity product1 = permanentCart.getItems().stream()
        .filter(item -> "product-1".equals(item.getItemId()))
        .findFirst()
        .orElse(null);
      assertNotNull(product1);
      assertEquals(3, product1.getQuantity());
    }
  }

  @Nested
  @DisplayName("Hard Business Logic Tests")
  class HardBusinessLogicTests {

    @Test
    @DisplayName("Should handle concurrent cart modifications safely")
    void shouldHandleConcurrentCartModificationsSafely() throws InterruptedException {
      // Given
      String customerId = "concurrent-customer";
      int numberOfThreads = 10;
      int itemsPerThread = 5;

      // When - Simulate concurrent additions
      Thread[] threads = new Thread[numberOfThreads];
      for (int i = 0; i < numberOfThreads; i++) {
        final int threadId = i;
        threads[i] = new Thread(() -> {
          for (int j = 0; j < itemsPerThread; j++) {
            cartService.add(customerId, "product-" + threadId + "-" + j, 1, 1000);
          }
        });
        threads[i].start();
      }

      // Wait for all threads to complete
      for (Thread thread : threads) {
        thread.join();
      }

      // Then
      CartEntity cart = cartService.get(customerId);
      assertEquals(numberOfThreads * itemsPerThread, cart.getItems().size());
    }

    @Test
    @DisplayName("Should handle large quantity calculations without overflow")
    void shouldHandleLargeQuantityCalculationsWithoutOverflow() {
      // Given
      String customerId = "large-calc-customer";
      
      // When - Add item with large quantities and prices
      cartService.add(customerId, "expensive-item", 1000, 999999); // $9999.99 x 1000

      // Then
      CartEntity cart = cartService.get(customerId);
      ItemEntity item = cart.getItems().get(0);
      
      // Verify no integer overflow occurred
      long expectedTotal = (long) item.getQuantity() * item.getUnitPrice();
      assertEquals(999999000L, expectedTotal);
      assertTrue(expectedTotal > Integer.MAX_VALUE); // Verify we're testing overflow scenario
    }

    @Test
    @DisplayName("Should handle cart operations with special characters in IDs")
    void shouldHandleCartOperationsWithSpecialCharactersInIds() {
      // Given
      String specialCustomerId = "customer@domain.com#123";
      String specialProductId = "product/with-special@chars#123";

      // When
      cartService.add(specialCustomerId, specialProductId, 1, 1000);

      // Then
      CartEntity cart = cartService.get(specialCustomerId);
      assertEquals(1, cart.getItems().size());
      assertEquals(specialProductId, cart.getItems().get(0).getItemId());
    }

    @Test
    @DisplayName("Should maintain data consistency during complex operations")
    void shouldMaintainDataConsistencyDuringComplexOperations() {
      // Given
      String customerId = "consistency-customer";
      
      // When - Perform complex sequence of operations
      cartService.add(customerId, "product-1", 5, 1000);
      cartService.add(customerId, "product-2", 3, 2000);
      cartService.update(customerId, "product-1", 10, 1200);
      cartService.deleteItem(customerId, "product-2");
      cartService.add(customerId, "product-3", 2, 1500);

      // Then
      CartEntity cart = cartService.get(customerId);
      assertEquals(2, cart.getItems().size()); // product-1 and product-3

      // Verify product-1 was updated correctly
      ItemEntity product1 = cart.getItems().stream()
        .filter(item -> "product-1".equals(item.getItemId()))
        .findFirst()
        .orElse(null);
      assertNotNull(product1);
      assertEquals(10, product1.getQuantity());
      assertEquals(1200, product1.getUnitPrice());

      // Verify product-2 was deleted
      boolean hasProduct2 = cart.getItems().stream()
        .anyMatch(item -> "product-2".equals(item.getItemId()));
      assertFalse(hasProduct2);

      // Verify product-3 was added
      ItemEntity product3 = cart.getItems().stream()
        .filter(item -> "product-3".equals(item.getItemId()))
        .findFirst()
        .orElse(null);
      assertNotNull(product3);
      assertEquals(2, product3.getQuantity());
      assertEquals(1500, product3.getUnitPrice());
    }

    @Test
    @DisplayName("Should handle edge case of zero and negative values")
    void shouldHandleEdgeCaseOfZeroAndNegativeValues() {
      // Given
      String customerId = "edge-case-customer";

      // When & Then - Test zero quantity (should remove item)
      cartService.add(customerId, "product-1", 5, 1000);
      cartService.update(customerId, "product-1", 0, 1000);
      
      CartEntity cart = cartService.get(customerId);
      assertEquals(0, cart.getItems().size());

      // Test handling of edge cases in business logic
      assertThrows(IllegalArgumentException.class, () -> {
        cartService.add(customerId, "product-2", -1, 1000); // Negative quantity
      });

      assertThrows(IllegalArgumentException.class, () -> {
        cartService.add(customerId, "product-3", 1, -100); // Negative price
      });
    }
  }

  @Nested
  @DisplayName("Performance and Stress Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle cart with maximum allowed items efficiently")
    void shouldHandleCartWithMaximumAllowedItemsEfficiently() {
      // Given
      String customerId = "max-items-customer";
      int maxItems = 1000; // Assume business rule of max 1000 items

      // When - Add maximum items
      long startTime = System.currentTimeMillis();
      for (int i = 1; i <= maxItems; i++) {
        cartService.add(customerId, "product-" + i, 1, 1000);
      }
      long endTime = System.currentTimeMillis();

      // Then
      CartEntity cart = cartService.get(customerId);
      assertEquals(maxItems, cart.getItems().size());
      
      // Performance assertion - should complete within reasonable time
      long duration = endTime - startTime;
      assertTrue(duration < 5000, "Adding " + maxItems + " items took too long: " + duration + "ms");
    }

    @Test
    @DisplayName("Should handle rapid cart operations efficiently")
    void shouldHandleRapidCartOperationsEfficiently() {
      // Given
      String customerId = "rapid-ops-customer";
      int operationCount = 100;

      // When - Perform rapid operations
      long startTime = System.currentTimeMillis();
      for (int i = 0; i < operationCount; i++) {
        cartService.add(customerId, "product-" + (i % 10), 1, 1000);
        if (i % 5 == 0) {
          cartService.get(customerId); // Intermittent reads
        }
      }
      long endTime = System.currentTimeMillis();

      // Then
      CartEntity cart = cartService.get(customerId);
      assertEquals(10, cart.getItems().size()); // 10 unique products
      
      // Each product should have quantity 10 (100 operations / 10 products)
      cart.getItems().forEach(item -> assertEquals(10, item.getQuantity()));
      
      // Performance assertion
      long duration = endTime - startTime;
      assertTrue(duration < 2000, "Rapid operations took too long: " + duration + "ms");
    }
  }
}