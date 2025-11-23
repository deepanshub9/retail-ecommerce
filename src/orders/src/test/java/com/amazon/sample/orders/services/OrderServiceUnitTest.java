/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.orders.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.amazon.sample.orders.entities.OrderEntity;
import com.amazon.sample.orders.entities.OrderItemEntity;
import com.amazon.sample.orders.entities.ShippingAddressEntity;
import com.amazon.sample.orders.messaging.OrdersEventHandler;
import com.amazon.sample.orders.repositories.OrderRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Unit Tests")
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrdersEventHandler eventHandler;

    @InjectMocks
    private OrderService orderService;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = createSampleOrder();
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        // Given
        String orderId = UUID.randomUUID().toString();
        sampleOrder.setId(orderId);
        
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(sampleOrder);

        // When
        OrderEntity result = orderService.create(sampleOrder);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        assertEquals(2, result.getItems().size());
        assertNotNull(result.getShippingAddress());
        
        verify(orderRepository, times(1)).save(sampleOrder);
    }

    @Test
    @DisplayName("Should handle order creation with single item")
    void shouldHandleOrderCreationWithSingleItem() {
        // Given
        OrderEntity singleItemOrder = new OrderEntity();
        singleItemOrder.setId(UUID.randomUUID().toString());
        singleItemOrder.setCreatedDate(LocalDateTime.now());
        
        OrderItemEntity item = new OrderItemEntity("single-product", 1, 5000, 5000);
        singleItemOrder.setItems(List.of(item));
        singleItemOrder.setShippingAddress(createSampleAddress());

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(singleItemOrder);

        // When
        OrderEntity result = orderService.create(singleItemOrder);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals("single-product", result.getItems().get(0).getProductId());
        assertEquals(5000, result.getItems().get(0).getTotal());
    }

    @Test
    @DisplayName("Should list all orders")
    void shouldListAllOrders() {
        // Given
        List<OrderEntity> expectedOrders = Arrays.asList(
            createSampleOrder(),
            createSampleOrder(),
            createSampleOrder()
        );
        
        when(orderRepository.findAll()).thenReturn(expectedOrders);

        // When
        List<OrderEntity> result = orderService.list();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no orders exist")
    void shouldReturnEmptyListWhenNoOrdersExist() {
        // Given
        when(orderRepository.findAll()).thenReturn(List.of());

        // When
        List<OrderEntity> result = orderService.list();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle repository exceptions during creation")
    void shouldHandleRepositoryExceptionsDuringCreation() {
        // Given
        when(orderRepository.save(any(OrderEntity.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.create(sampleOrder);
        });
        
        verify(orderRepository, times(1)).save(sampleOrder);
    }

    @Test
    @DisplayName("Should handle repository exceptions during listing")
    void shouldHandleRepositoryExceptionsDuringListing() {
        // Given
        when(orderRepository.findAll())
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.list();
        });
        
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should calculate order totals correctly")
    void shouldCalculateOrderTotalsCorrectly() {
        // Given
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        
        List<OrderItemEntity> items = Arrays.asList(
            new OrderItemEntity("product-1", 2, 1000, 2000), // $10.00 x 2 = $20.00
            new OrderItemEntity("product-2", 3, 1500, 4500), // $15.00 x 3 = $45.00
            new OrderItemEntity("product-3", 1, 2500, 2500)  // $25.00 x 1 = $25.00
        );
        order.setItems(items);
        order.setShippingAddress(createSampleAddress());

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);

        // When
        OrderEntity result = orderService.create(order);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getItems().size());
        
        int totalAmount = result.getItems().stream()
            .mapToInt(OrderItemEntity::getTotal)
            .sum();
        assertEquals(9000, totalAmount); // $90.00 total
    }

    @Test
    @DisplayName("Should preserve order creation timestamp")
    void shouldPreserveOrderCreationTimestamp() {
        // Given
        LocalDateTime creationTime = LocalDateTime.now().minusMinutes(5);
        sampleOrder.setCreatedDate(creationTime);
        
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(sampleOrder);

        // When
        OrderEntity result = orderService.create(sampleOrder);

        // Then
        assertNotNull(result);
        assertEquals(creationTime, result.getCreatedDate());
    }

    private OrderEntity createSampleOrder() {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setCreatedDate(LocalDateTime.now());
        
        List<OrderItemEntity> items = Arrays.asList(
            new OrderItemEntity("product-1", 2, 2999, 5998),
            new OrderItemEntity("product-2", 1, 4999, 4999)
        );
        order.setItems(items);
        order.setShippingAddress(createSampleAddress());
        
        return order;
    }

    private ShippingAddressEntity createSampleAddress() {
        return new ShippingAddressEntity(
            "John",
            "Doe", 
            "john.doe@example.com",
            "123 Main Street",
            "Apt 4B",
            "Seattle",
            "98101",
            "WA"
        );
    }
}