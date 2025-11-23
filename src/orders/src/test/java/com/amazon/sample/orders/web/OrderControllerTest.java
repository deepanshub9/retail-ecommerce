/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

package com.amazon.sample.orders.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.amazon.sample.orders.entities.OrderEntity;
import com.amazon.sample.orders.entities.OrderItemEntity;
import com.amazon.sample.orders.entities.ShippingAddressEntity;
import com.amazon.sample.orders.services.OrderService;
import com.amazon.sample.orders.web.payload.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@DisplayName("Order Controller Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderMapper orderMapper;

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        String orderId = UUID.randomUUID().toString();
        OrderEntity orderEntity = createSampleOrderEntity(orderId);
        
        when(orderMapper.toOrderEntity(any())).thenReturn(orderEntity);
        when(orderService.create(any(OrderEntity.class))).thenReturn(orderEntity);
        when(orderMapper.toExistingOrder(any())).thenReturn(createExistingOrderResponse(orderId));

        String orderJson = """
            {
                "items": [
                    {
                        "productId": "product-1",
                        "quantity": 2,
                        "unitPrice": 25.99
                    }
                ],
                "shippingAddress": {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "address1": "123 Main St",
                    "city": "Seattle",
                    "zip": "98101",
                    "state": "WA"
                }
            }
            """;

        // When & Then
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value("product-1"))
                .andExpect(jsonPath("$.shippingAddress.firstName").value("John"));
    }

    @Test
    @DisplayName("Should return empty list when no orders exist")
    void shouldReturnEmptyListWhenNoOrdersExist() throws Exception {
        // Given
        when(orderService.list()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Should return list of orders")
    void shouldReturnListOfOrders() throws Exception {
        // Given
        String orderId1 = UUID.randomUUID().toString();
        String orderId2 = UUID.randomUUID().toString();
        
        List<OrderEntity> orders = Arrays.asList(
            createSampleOrderEntity(orderId1),
            createSampleOrderEntity(orderId2)
        );

        when(orderService.list()).thenReturn(orders);
        when(orderMapper.toExistingOrder(any())).thenReturn(
            createExistingOrderResponse(orderId1),
            createExistingOrderResponse(orderId2)
        );

        // When & Then
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(orderId1))
                .andExpect(jsonPath("$[1].id").value(orderId2));
    }

    private OrderEntity createSampleOrderEntity(String orderId) {
        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setCreatedDate(LocalDateTime.now());
        
        OrderItemEntity item = new OrderItemEntity("product-1", 2, 2599, 5198);
        order.setItems(List.of(item));
        
        ShippingAddressEntity address = new ShippingAddressEntity(
            "John", "Doe", "john.doe@example.com",
            "123 Main St", "", "Seattle", "98101", "WA"
        );
        order.setShippingAddress(address);
        
        return order;
    }

    private com.amazon.sample.orders.web.payload.ExistingOrder createExistingOrderResponse(String orderId) {
        com.amazon.sample.orders.web.payload.ExistingOrder response = 
            new com.amazon.sample.orders.web.payload.ExistingOrder();
        response.setId(orderId);
        response.setCreatedDate(LocalDateTime.now());
        
        com.amazon.sample.orders.web.payload.OrderItem item = 
            new com.amazon.sample.orders.web.payload.OrderItem();
        item.setProductId("product-1");
        item.setQuantity(2);
        item.setUnitPrice(2599);
        response.setItems(List.of(item));
        
        com.amazon.sample.orders.web.payload.ShippingAddress address = 
            new com.amazon.sample.orders.web.payload.ShippingAddress();
        address.setFirstName("John");
        address.setLastName("Doe");
        address.setEmail("john.doe@example.com");
        address.setAddress1("123 Main St");
        address.setCity("Seattle");
        address.setZip("98101");
        address.setState("WA");
        response.setShippingAddress(address);
        
        return response;
    }
}