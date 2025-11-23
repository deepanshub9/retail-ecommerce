# UI Service - Test Suite Documentation

## Overview
The UI service now includes comprehensive test coverage with easy to medium difficulty levels, covering unit tests, integration tests, and reactive web testing with Spring WebFlux and modern Java testing practices.

## Test Structure

### 1. **Controller Tests** (`web/CatalogControllerTest.java`)
**Difficulty: Easy to Medium**
- WebFlux controller testing with `@WebFluxTest`
- Tests Thymeleaf template rendering
- Validates service integration and error handling
- Uses `WebTestClient` for reactive testing

### 2. **Service Layer Tests** (`services/catalog/CatalogServiceTest.java`)
**Difficulty: Medium**
- Reactive service testing with `StepVerifier`
- Tests Kiota-generated client integration
- Validates data transformation and mapping
- Covers error scenarios and edge cases

### 3. **Integration Tests** (`web/IntegrationTest.java`)
**Difficulty: Medium**
- End-to-end testing with `MockWebServer`
- Tests complete request/response cycles
- Validates service orchestration
- Includes concurrent request testing

### 4. **Application Context Tests** (`UiApplicationTests.java`)
**Difficulty: Easy**
- Spring Boot application startup testing
- Bean configuration validation
- Context loading verification

## Running Tests

### Prerequisites
```bash
# Java 21 required
java -version

# Maven wrapper included
./mvnw --version
```

### Test Commands

```bash
# Install dependencies
make deps

# Generate API clients (required before testing)
make generate-clients

# Run all tests
make test

# Run specific test categories
make test-unit           # Unit tests only
make test-integration    # Integration tests only

# Run with coverage
make test-coverage      # Generates coverage report

# Individual test execution
./mvnw test -Dtest=CatalogControllerTest
./mvnw test -Dtest=CatalogServiceTest
./mvnw test -Dtest=IntegrationTest
```

## Test Features

### 🎯 **Easy Level Tests**
- Basic controller endpoint testing
- Simple service method validation
- Application context loading
- Bean configuration verification

### 🎯 **Medium Level Tests**
- Reactive programming patterns
- Service integration testing
- Mock external service integration
- Error handling and resilience
- Concurrent request scenarios

### 🔧 **Test Technologies**
- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **WebTestClient** - Reactive web testing
- **StepVerifier** - Reactive stream testing
- **MockWebServer** - HTTP service mocking
- **Spring Boot Test** - Framework-specific testing

## Test Coverage Areas

### ✅ **Functional Testing**
- Web controller endpoints
- Service layer business logic
- Template rendering (Thymeleaf)
- API client integration

### ✅ **Integration Testing**
- External service communication
- End-to-end request flows
- Service orchestration
- Error propagation

### ✅ **Reactive Testing**
- Mono/Flux stream validation
- Backpressure handling
- Error signal propagation
- Async operation testing

## Dependencies Added

```xml
<!-- Enhanced test dependencies -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

## Test Configuration

### Test Profiles (`application-test.yml`)
```yaml
spring:
  profiles:
    active: test

endpoints:
  catalog: http://localhost:8081
  cart: http://localhost:8082
  checkout: http://localhost:8083

chat:
  provider: mock
```

## Reactive Testing Patterns

### Service Testing with StepVerifier
```java
StepVerifier.create(catalogService.getProducts("", "", 1, 6))
    .assertNext(productPage -> {
        assertNotNull(productPage);
        assertEquals(2, productPage.getProducts().size());
    })
    .verifyComplete();
```

### Controller Testing with WebTestClient
```java
webTestClient.get()
    .uri("/catalog")
    .exchange()
    .expectStatus().isOk()
    .expectBody(String.class)
    .value(body -> assert body.contains("Product 1"));
```

## Mock Service Integration

### MockWebServer for External Services
```java
mockCatalogService.enqueue(new MockResponse()
    .setBody(mockProductsResponse)
    .addHeader("Content-Type", "application/json"));
```

## Continuous Integration

The Makefile includes a complete CI pipeline:
```bash
make ci  # Runs: clean -> deps -> generate-clients -> analyze -> test-coverage -> package
```

## Performance Testing

Performance tests measure:
- Template rendering speed
- Service response times
- Concurrent request handling
- Memory usage patterns

Run performance tests:
```bash
make test-performance
make load-test  # Basic load testing with curl
```

## Client Generation Testing

Tests validate:
- **Kiota-generated clients** work correctly
- **API contract compliance** with backend services
- **Error handling** in generated code
- **Serialization/deserialization** accuracy

## Best Practices Demonstrated

1. **Reactive Testing**: Proper use of StepVerifier for reactive streams
2. **Web Layer Testing**: Comprehensive WebFlux controller testing
3. **Service Mocking**: External service simulation with MockWebServer
4. **Test Profiles**: Environment-specific test configuration
5. **Integration Testing**: End-to-end workflow validation
6. **Error Testing**: Comprehensive error scenario coverage
7. **Concurrent Testing**: Multi-threaded request validation

## Development Workflow

1. **Generate Clients**: Run `make generate-clients` after API changes
2. **Write Tests First**: TDD approach for new features
3. **Run Tests Locally**: Use `make test` before commits
4. **Check Coverage**: Maintain >80% test coverage
5. **Integration Validation**: Run full integration tests

## Troubleshooting

### Common Issues
- **Client Generation**: Ensure OpenAPI specs are valid
- **MockWebServer**: Check port availability for mock services
- **Reactive Tests**: Verify proper StepVerifier usage
- **Template Tests**: Ensure Thymeleaf templates exist

### Debug Commands
```bash
# Verbose test output
./mvnw test -X

# Run single test with debug
./mvnw test -Dtest=CatalogControllerTest -Dmaven.surefire.debug

# Generate clients manually
./mvnw kiota:generate

# Check test reports
open target/site/surefire-report.html
```

## Test Data Management

Tests use:
- **Mock product data** for catalog testing
- **Realistic user scenarios** for integration tests
- **Error simulation** for resilience testing
- **Performance baselines** for load testing

This comprehensive test suite ensures the UI service maintains high quality and reliability standards while demonstrating modern Spring WebFlux testing practices and reactive programming patterns.