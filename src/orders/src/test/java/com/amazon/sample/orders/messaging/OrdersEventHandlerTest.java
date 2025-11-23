/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.orders.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.amazon.sample.orders.entities.OrderEntity;
import com.amazon.sample.orders.entities.OrderItemEntity;
import com.amazon.sample.orders.entities.ShippingAddressEntity;
import java.time.LocalDateTime;
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
@DisplayName("Orders Event Handler Tests")
class OrdersEventHandlerTest {

    @Mock
    private MessagingProvider messagingProvider;

    @InjectMocks
    private OrdersEventHandler eventHandler;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = createSampleOrder();
    }

    @Test
    @DisplayName("Should publish order created event successfully")
    void shouldPublishOrderCreatedEventSuccessfully() {
        // Given
        doNothing().when(messagingProvider).postCreatedEvent(any(OrderEntity.class));

        // When
        assertDoesNotThrow(() -> {
            eventHandler.postCreatedEvent(sampleOrder);
        });

        // Then
        verify(messagingProvider, times(1)).postCreatedEvent(sampleOrder);
    }

    @Test
    @DisplayName("Should handle messaging provider exceptions gracefully")
    void shouldHandleMessagingProviderExceptionsGracefully() {
        // Given
        doThrow(new RuntimeException("Message queue unavailable"))
            .when(messagingProvider).postCreatedEvent(any(OrderEntity.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            eventHandler.postCreatedEvent(sampleOrder);
        });

        verify(messagingProvider, times(1)).postCreatedEvent(sampleOrder);
    }

    @Test
    @DisplayName("Should publish events for orders with multiple items")
    void shouldPublishEventsForOrdersWithMultipleItems() {
        // Given
        OrderEntity multiItemOrder = createOrderWithMultipleItems();
        doNothing().when(messagingProvider).postCreatedEvent(any(OrderEntity.class));

        // When
        eventHandler.postCreatedEvent(multiItemOrder);

        // Then
        verify(messagingProvider, times(1)).postCreatedEvent(multiItemOrder);
        assertEquals(3, multiItemOrder.getItems().size());
    }

    private OrderEntity createSampleOrder() {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setCreatedDate(LocalDateTime.now());
        
        List<OrderItemEntity> items = List.of(
            new OrderItemEntity("product-1", 1, 2999, 2999),
            new OrderItemEntity("product-2", 2, 1999, 3998)
        );
        order.setItems(items);
        
        ShippingAddressEntity address = new ShippingAddressEntity(
            "John", "Doe", "john.doe@example.com",
            "123 Main St", "", "Seattle", "98101", "WA"
        );
        order.setShippingAddress(address);
        
        return order;
    }

    private OrderEntity createOrderWithMultipleItems() {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setCreatedDate(LocalDateTime.now());
        
        List<OrderItemEntity> items = List.of(
            new OrderItemEntity("product-1", 1, 1000, 1000),
            new OrderItemEntity("product-2", 2, 1500, 3000),
            new OrderItemEntity("product-3", 3, 2000, 6000)
        );
        order.setItems(items);
        
        ShippingAddressEntity address = new ShippingAddressEntity(
            "Jane", "Smith", "jane.smith@example.com",
            "456 Oak Ave", "Suite 200", "Portland", "97201", "OR"
        );
        order.setShippingAddress(address);
        
        return order;
    }
}