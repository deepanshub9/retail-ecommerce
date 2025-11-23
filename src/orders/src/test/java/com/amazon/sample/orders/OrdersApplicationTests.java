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

package com.amazon.sample.orders;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
@DisplayName("Orders Application Tests")
class OrdersApplicationTests {

  @Test
  @DisplayName("Should load Spring context successfully")
  void contextLoads() {
    // This test verifies that the Spring application context loads without errors
    // If this test passes, it means all beans are properly configured
  }

  @Test
  @DisplayName("Should have required beans in context")
  void shouldHaveRequiredBeansInContext(@Autowired ApplicationContext context) {
    // Verify core service beans are present
    assertThat(context.containsBean("orderService")).isTrue();
    assertThat(context.containsBean("orderRepository")).isTrue();
    assertThat(context.containsBean("orderController")).isTrue();
  }

  @Test
  @DisplayName("Should configure database properties correctly")
  void shouldConfigureDatabasePropertiesCorrectly(@Autowired DataSource dataSource) {
    assertThat(dataSource).isNotNull();
  }
}
