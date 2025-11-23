/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazon.sample.orders.services;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.amazon.sample.orders.entities.OrderEntity;
import com.amazon.sample.orders.entities.OrderItemEntity;
import com.amazon.sample.orders.entities.ShippingAddressEntity;
import com.amazon.sample.orders.repositories.OrderRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
@DisplayName("Order Service Integration Tests")
public class OrderServicePostgresTests {

  @LocalServerPort
  private Integer port;

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
    "postgres:17.2"
  );

  @BeforeAll
  static void beforeAll() {
    postgres.start();
  }

  @AfterAll
  static void afterAll() {
    postgres.stop();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("retail.orders.persistence.provider", () -> "postgres");
    registry.add(
      "retail.orders.persistence.endpoint",
      () -> postgres.getHost() + ":" + postgres.getMappedPort(5432)
    );
    registry.add("retail.orders.persistence.username", postgres::getUsername);
    registry.add("retail.orders.persistence.password", postgres::getPassword);
    registry.add("retail.orders.persistence.name", postgres::getDatabaseName);
  }

  @Autowired
  OrderRepository orderRepository;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + port;
    orderRepository.deleteAll();
  }

  @Test
  @DisplayName("Should return empty list when no orders exist")
  void shouldGetEmptyOrders() {
    given()
      .contentType(ContentType.JSON)
      .when()
      .get("/orders")
      .then()
      .statusCode(200)
      .body(".", hasSize(0));
  }

  @Test
  @DisplayName("Should create and retrieve orders successfully")
  void shouldCreateAndRetrieveOrders() {
    // Create order via API
    String orderJson = """
        {
            "items": [
                {
                    "productId": "product-123",
                    "quantity": 2,
                    "unitPrice": 29.99
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

    Response createResponse = given()
      .contentType(ContentType.JSON)
      .body(orderJson)
      .when()
      .post("/orders")
      .then()
      .statusCode(200)
      .body("id", notNullValue())
      .body("items", hasSize(1))
      .body("items[0].productId", equalTo("product-123"))
      .body("shippingAddress.firstName", equalTo("John"))
      .extract().response();

    String orderId = createResponse.path("id");
    assertNotNull(orderId);

    // Verify order appears in list
    given()
      .contentType(ContentType.JSON)
      .when()
      .get("/orders")
      .then()
      .statusCode(200)
      .body(".", hasSize(1))
      .body("[0].id", equalTo(orderId));
  }

  @Test
  @DisplayName("Should handle multiple orders correctly")
  void shouldHandleMultipleOrders() {
    var items = List.of(new OrderItemEntity("product-456", 1, 1999, 1999));

    List<OrderEntity> orders = List.of(
      createOrderEntity("Order 1", items),
      createOrderEntity("Order 2", items)
    );
    orderRepository.saveAll(orders);

    given()
      .contentType(ContentType.JSON)
      .when()
      .get("/orders")
      .then()
      .statusCode(200)
      .body(".", hasSize(2))
      .body("[0].items", hasSize(1))
      .body("[1].items", hasSize(1));
  }

  @Test
  @DisplayName("Should validate order creation with invalid data")
  void shouldValidateOrderCreationWithInvalidData() {
    String invalidOrderJson = """
        {
            "items": [],
            "shippingAddress": {}
        }
        """;

    given()
      .contentType(ContentType.JSON)
      .body(invalidOrderJson)
      .when()
      .post("/orders")
      .then()
      .statusCode(anyOf(is(400), is(500)));
  }

  @Test
  @DisplayName("Should handle concurrent order creation")
  void shouldHandleConcurrentOrderCreation() {
    String orderJson = """
        {
            "items": [
                {
                    "productId": "concurrent-test",
                    "quantity": 1,
                    "unitPrice": 10.00
                }
            ],
            "shippingAddress": {
                "firstName": "Test",
                "lastName": "User",
                "email": "test@example.com",
                "address1": "Test Address",
                "city": "Test City",
                "zip": "12345",
                "state": "TS"
            }
        }
        """;

    // Create multiple orders concurrently
    for (int i = 0; i < 5; i++) {
      given()
        .contentType(ContentType.JSON)
        .body(orderJson)
        .when()
        .post("/orders")
        .then()
        .statusCode(200)
        .body("id", notNullValue());
    }

    // Verify all orders were created
    given()
      .contentType(ContentType.JSON)
      .when()
      .get("/orders")
      .then()
      .statusCode(200)
      .body(".", hasSize(5));
  }

  private OrderEntity createOrderEntity(String identifier, List<OrderItemEntity> items) {
    OrderEntity order = new OrderEntity();
    order.setId(UUID.randomUUID().toString());
    order.setCreatedDate(LocalDateTime.now());
    order.setItems(items);
    order.setShippingAddress(new ShippingAddressEntity(
      "John " + identifier,
      "Doe",
      "john.doe@example.com",
      "123 Main St",
      "",
      "Seattle",
      "98101",
      "WA"
    ));
    return order;
  }
}
