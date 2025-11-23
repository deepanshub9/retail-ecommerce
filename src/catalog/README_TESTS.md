# Catalog Service - Test Suite Documentation

## Overview
The catalog service now includes comprehensive test coverage with easy to medium difficulty levels, covering unit tests, integration tests, and performance benchmarks.

## Test Structure

### 1. **API Layer Tests** (`test/api_test.go`)
**Difficulty: Easy to Medium**
- Tests business logic layer with mocked repository
- Covers all API methods: GetProducts, GetProduct, GetTags, GetSize
- Uses testify/mock for dependency injection
- Tests both success and error scenarios

### 2. **Controller Tests** (`test/controller_test.go`)
**Difficulty: Medium**
- HTTP endpoint testing with Gin framework
- Tests pagination, filtering, sorting, and error handling
- Validates JSON response formats
- Covers edge cases like invalid parameters

### 3. **Repository Tests** (`test/repository_test.go`)
**Difficulty: Medium**
- Database layer testing with test suite pattern
- Uses in-memory SQLite for fast testing
- Tests CRUD operations, filtering, and relationships
- Includes performance benchmarks

### 4. **Integration Tests** (`test/integration_test.go`)
**Difficulty: Medium**
- End-to-end workflow testing
- Tests complete user journeys
- Validates API consistency and performance
- Includes concurrent request testing

### 5. **Configuration Tests** (`test/config_test.go`)
**Difficulty: Easy**
- Environment variable loading
- Default value validation
- Configuration edge cases

### 6. **Chaos Engineering Tests** (`test/chaos_middleware_test.go`)
**Difficulty: Medium**
- Tests fault injection capabilities
- Latency injection testing
- Error status simulation
- Health check manipulation

## Running Tests

### Prerequisites
```bash
# Install Go (if not installed)
# Download from https://golang.org/dl/

# Install test dependencies
go mod tidy
```

### Test Commands

```bash
# Run all tests
make test

# Run specific test categories
make test-unit           # Unit tests only
make test-integration    # Integration tests only
make test-chaos         # Chaos engineering tests
make test-benchmark     # Performance benchmarks

# Run with coverage
make test-coverage      # Generates coverage.html report

# Individual test files
go test -v ./test -run TestCatalogAPI        # API tests
go test -v ./test -run TestCatalogEndpoints  # Controller tests
go test -v ./test -run TestRepositoryTestSuite # Repository tests
go test -v ./test -run TestIntegrationTestSuite # Integration tests
go test -v ./test -run TestConfiguration     # Config tests
```

## Test Features

### 🎯 **Easy Level Tests**
- Basic API functionality
- Configuration loading
- Simple CRUD operations
- Health check validation

### 🎯 **Medium Level Tests**
- Complex filtering and pagination
- Error handling scenarios
- Performance benchmarking
- Concurrent request handling
- Chaos engineering scenarios

### 🔧 **Test Utilities**
- Mock repository implementation
- Test data factories
- HTTP request helpers
- Database test fixtures

## Test Coverage Areas

### ✅ **Functional Testing**
- All API endpoints
- Database operations
- Configuration management
- Error handling

### ✅ **Non-Functional Testing**
- Performance benchmarks
- Concurrent access
- Response time validation
- Memory usage optimization

### ✅ **Reliability Testing**
- Chaos engineering
- Fault injection
- Recovery scenarios
- Health monitoring

## Dependencies Added

```go
// Test-specific dependencies
github.com/DATA-DOG/go-sqlmock v1.5.2    // SQL mocking
github.com/golang/mock v1.6.0             // General mocking
github.com/stretchr/testify v1.10.0       // Test assertions
github.com/testcontainers/testcontainers-go v0.35.0 // Container testing
```

## Test Data

The tests use the existing product and tag JSON files:
- `repository/products.json` - Sample product catalog
- `repository/tags.json` - Product categories

## Continuous Integration

The Makefile includes a complete CI pipeline:
```bash
make ci  # Runs: deps -> quality checks -> test coverage
```

## Performance Benchmarks

Benchmark tests measure:
- Product retrieval performance
- Database query optimization
- Memory allocation patterns
- Concurrent request handling

Run benchmarks:
```bash
go test -v ./test -bench=. -benchmem
```

## Best Practices Demonstrated

1. **Test Organization**: Clear separation of test types
2. **Mocking**: Proper dependency injection and mocking
3. **Test Suites**: Organized test execution with setup/teardown
4. **Coverage**: Comprehensive test coverage reporting
5. **Performance**: Benchmark testing for optimization
6. **Integration**: End-to-end workflow validation
7. **Reliability**: Chaos engineering for resilience testing

## Development Workflow

1. **Write Tests First**: TDD approach encouraged
2. **Run Tests Locally**: Use `make test` before commits
3. **Check Coverage**: Maintain >80% test coverage
4. **Performance Testing**: Regular benchmark execution
5. **Integration Validation**: End-to-end testing before deployment

This test suite provides a solid foundation for maintaining code quality and ensuring the catalog service works reliably in production environments.