# Orders Service - Test Suite Documentation

## Overview
The Orders service now includes comprehensive test coverage with easy to medium difficulty levels, covering unit tests, integration tests, event-driven messaging, and performance testing.

## Test Structure

### 1. **Controller Tests** (`web/OrderControllerTest.java`)
**Difficulty: Easy to Medium**
- HTTP endpoint testing with MockMvc
- Tests order creation and retrieval endpoints
- Validates JSON request/response handling
- Covers error scenarios and validation

### 2. **Service Layer Tests** (`services/OrderServiceUnitTest.java`)
**Difficulty: Easy to Medium**
- Business logic testing with mocked dependencies
- Tests order creation, listing, and validation
- Covers exception handling and edge cases
- Validates order calculations and data integrity

### 3. **Integration Tests** (`services/OrderServicePostgresTests.java`)
**Difficulty: Medium**
- End-to-end testing with TestContainers PostgreSQL
- Tests complete API workflows
- Validates database persistence and transactions
- Includes concurrent request testing

### 4. **Event Handler Tests** (`messaging/OrdersEventHandlerTest.java`)
**Difficulty: Medium**
- Event-driven messaging testing
- Tests order event publishing
- Validates messaging provider integration
- Covers async processing scenarios

### 5. **Application Context Tests** (`OrdersApplicationTests.java`)
**Difficulty: Easy**
- Spring Boot application startup testing
- Bean configuration validation
- Context loading verification

### 6. **Chaos Engineering Tests** (`chaos/ChaosIntegrationTests.java`)
**Difficulty: Medium**
- Fault injection testing
- Latency and error simulation
- Resilience validation

### 7. **Metrics Tests** (`metrics/OrdersMetricsTests.java`)
**Difficulty: Easy to Medium**
- Micrometer metrics validation
- Counter and gauge testing
- Performance monitoring verification

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
# Run all tests
make test

# Run specific test categories
make test-unit           # Unit tests only
make test-integration    # Integration tests only
make test-chaos         # Chaos engineering tests
make test-performance   # Performance tests

# Run with coverage
make test-coverage      # Generates coverage report

# Individual test execution
./mvnw test -Dtest=OrderControllerTest
./mvnw test -Dtest=OrderServiceUnitTest
./mvnw test -Dtest=OrderServicePostgresTests
./mvnw test -Dtest=OrdersEventHandlerTest
```

## Test Features

### 🎯 **Easy Level Tests**
- Basic CRUD operations
- Simple validation scenarios
- Application context loading
- Basic metrics verification

### 🎯 **Medium Level Tests**
- Complex business logic scenarios
- Event-driven messaging
- Database integration with TestContainers
- Concurrent request handling
- Chaos engineering scenarios

### 🔧 **Test Technologies**
- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **TestContainers** - Integration testing with real databases
- **RestAssured** - API testing
- **Spring Boot Test** - Spring-specific testing features
- **Awaitility** - Async testing utilities

## Test Coverage Areas

### ✅ **Functional Testing**
- Order creation and retrieval
- Data validation and persistence
- Event publishing and handling
- API endpoint functionality

### ✅ **Integration Testing**
- Database operations with PostgreSQL
- Message queue integration
- Complete request/response cycles
- Cross-service communication

### ✅ **Reliability Testing**
- Chaos engineering scenarios
- Concurrent request handling
- Error recovery mechanisms
- Performance under load

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
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <scope>test</scope>
</dependency>
```

## Test Data

Tests use realistic order data:
- Multiple product items per order
- Complete shipping address information
- Proper price calculations
- Realistic timestamps and IDs

## Continuous Integration

The Makefile includes a complete CI pipeline:
```bash
make ci  # Runs: clean -> deps -> analyze -> test-coverage -> package
```

## Performance Testing

Performance tests measure:
- Order creation throughput
- Database query performance
- Event publishing latency
- Concurrent request handling

Run performance tests:
```bash
make test-performance
make load-test  # Basic load testing with curl
```

## Event-Driven Testing

Tests validate:
- Order created events
- Message queue integration
- Event handler functionality
- Async processing reliability

## Database Testing

Integration tests use:
- **TestContainers PostgreSQL** for realistic database testing
- **Flyway migrations** for schema management
- **Transactional rollback** for test isolation
- **Connection pooling** validation

## Best Practices Demonstrated

1. **Test Organization**: Clear separation by layer and functionality
2. **Mocking Strategy**: Proper use of mocks vs real components
3. **Test Data Management**: Realistic and maintainable test data
4. **Async Testing**: Proper handling of event-driven scenarios
5. **Integration Testing**: Real database and messaging integration
6. **Performance Testing**: Load and stress testing capabilities
7. **Chaos Engineering**: Fault injection and resilience testing

## Development Workflow

1. **Write Tests First**: TDD approach for new features
2. **Run Tests Locally**: Use `make test` before commits
3. **Check Coverage**: Maintain >80% test coverage
4. **Integration Validation**: Run full integration tests
5. **Performance Baseline**: Regular performance testing

## Troubleshooting

### Common Issues
- **TestContainers**: Ensure Docker is running
- **Database Tests**: Check PostgreSQL container startup
- **Messaging Tests**: Verify RabbitMQ availability
- **Performance Tests**: Adjust timeouts for slower systems

### Debug Commands
```bash
# Verbose test output
./mvnw test -X

# Run single test with debug
./mvnw test -Dtest=OrderControllerTest -Dmaven.surefire.debug

# Check test reports
open target/surefire-reports/index.html
```

This comprehensive test suite ensures the Orders service maintains high quality and reliability standards while demonstrating modern Java testing practices and enterprise-grade testing strategies.