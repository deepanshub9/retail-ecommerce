# Checkout Service - Test Suite Documentation

## Overview
The Checkout service now includes comprehensive test coverage with easy to medium difficulty levels, covering unit tests, integration tests, and end-to-end testing with TypeScript and NestJS best practices.

## Test Structure

### 1. **Service Layer Tests** (`checkout.service.spec.ts`)
**Difficulty: Easy to Medium**
- Business logic testing with mocked dependencies
- Tests checkout calculations, tax, and shipping logic
- Covers order submission workflow
- Validates data persistence and retrieval

### 2. **Controller Tests** (`checkout.controller.spec.ts`)
**Difficulty: Easy to Medium**
- HTTP endpoint testing with NestJS testing utilities
- Tests request/response handling
- Validates parameter extraction and error handling
- Covers all REST endpoints

### 3. **Repository Tests** (`checkout-repository.spec.ts`)
**Difficulty: Medium**
- Tests both InMemory and Redis repository implementations
- Validates interface compliance
- Tests concurrent operations and error handling
- Covers data persistence patterns

### 4. **Integration Tests** (`app.e2e-spec.ts`)
**Difficulty: Medium**
- End-to-end testing with TestContainers Redis
- Tests complete checkout workflows
- Validates API integration and data flow
- Includes concurrent request testing

### 5. **Chaos Engineering Tests** (`chaos.e2e-spec.ts`)
**Difficulty: Medium**
- Fault injection testing
- Latency and error simulation
- Health check manipulation
- Resilience validation

## Running Tests

### Prerequisites
```bash
# Node.js 18+ required
node --version

# Yarn package manager
yarn --version

# Docker for TestContainers
docker --version
```

### Test Commands

```bash
# Install dependencies
make deps

# Run all tests
make test

# Run specific test categories
make test-unit           # Unit tests only
make test-integration    # Integration tests only
make test-e2e           # End-to-end tests only
make test-chaos         # Chaos engineering tests

# Run with coverage
make test-coverage      # Generates coverage report

# Individual test execution
yarn test checkout.service.spec.ts
yarn test checkout.controller.spec.ts
yarn test app.e2e-spec.ts
```

## Test Features

### 🎯 **Easy Level Tests**
- Basic CRUD operations
- Simple validation scenarios
- Service method testing
- Controller endpoint testing

### 🎯 **Medium Level Tests**
- Complex business logic scenarios
- Repository pattern testing
- Integration with external services
- Concurrent request handling
- Error recovery mechanisms

### 🔧 **Test Technologies**
- **Jest** - Test framework and mocking
- **NestJS Testing** - Framework-specific testing utilities
- **TestContainers** - Integration testing with real Redis
- **Supertest** - HTTP endpoint testing
- **TypeScript** - Type-safe testing

## Test Coverage Areas

### ✅ **Functional Testing**
- Checkout creation and updates
- Price calculations (subtotal, tax, shipping)
- Order submission workflow
- Data persistence and retrieval

### ✅ **Integration Testing**
- Redis integration with TestContainers
- HTTP API endpoint testing
- Service-to-service communication
- Complete checkout workflows

### ✅ **Reliability Testing**
- Chaos engineering scenarios
- Error handling and recovery
- Concurrent operation safety
- Input validation

## Dependencies Added

```json
{
  "devDependencies": {
    "@types/redis": "^4.0.11",
    "nock": "^13.5.5",
    "redis-memory-server": "^0.10.0",
    "testcontainers": "^11.0.3"
  }
}
```

## Test Data

Tests use realistic checkout scenarios:
- Multiple product items with different quantities
- Complete shipping address information
- Various delivery options
- Proper price calculations with tax and shipping

## Continuous Integration

The Makefile includes a complete CI pipeline:
```bash
make ci  # Runs: deps -> lint -> type-check -> test-coverage -> build
```

## Performance Testing

Performance tests measure:
- Checkout calculation speed
- Redis operation latency
- Concurrent request handling
- Memory usage patterns

Run performance tests:
```bash
make test-performance
make load-test  # Basic load testing with curl
```

## Integration Testing with TestContainers

Integration tests use:
- **Redis TestContainer** for realistic caching testing
- **Automatic container lifecycle** management
- **Port mapping** for service communication
- **Wait strategies** for container readiness

## Mocking Strategies

### Service Layer Mocking
```typescript
const mockRepository = {
  get: jest.fn(),
  set: jest.fn(),
  remove: jest.fn(),
};

const mockOrdersService = {
  create: jest.fn(),
};
```

### HTTP Mocking with Nock
```typescript
nock('http://orders-service')
  .post('/orders')
  .reply(200, { id: 'order123' });
```

## Best Practices Demonstrated

1. **Test Organization**: Clear separation by layer and functionality
2. **Dependency Injection**: Proper mocking of dependencies
3. **Async Testing**: Proper handling of promises and async operations
4. **Error Testing**: Comprehensive error scenario coverage
5. **Integration Testing**: Real external service integration
6. **Type Safety**: Full TypeScript coverage in tests
7. **Test Data Management**: Realistic and maintainable test scenarios

## Development Workflow

1. **Write Tests First**: TDD approach for new features
2. **Run Tests Locally**: Use `make test` before commits
3. **Check Coverage**: Maintain >80% test coverage
4. **Integration Validation**: Run full e2e tests
5. **Performance Baseline**: Regular performance testing

## Troubleshooting

### Common Issues
- **TestContainers**: Ensure Docker is running and accessible
- **Redis Tests**: Check Redis container startup logs
- **Port Conflicts**: Ensure test ports are available
- **Memory Issues**: Adjust Jest memory settings for large test suites

### Debug Commands
```bash
# Verbose test output
yarn test --verbose

# Run single test file
yarn test checkout.service.spec.ts

# Debug mode
yarn test:debug

# Watch mode for development
yarn test --watch
```

### Environment Variables
```bash
# Test configuration
NODE_ENV=test
PERSISTENCE_PROVIDER=in-memory
REDIS_URL=redis://localhost:6379
```

## Test Reports

Coverage reports are generated in:
- `coverage/lcov-report/index.html` - HTML coverage report
- `coverage/lcov.info` - LCOV format for CI integration
- `coverage/coverage-final.json` - JSON format for tooling

## Mock Services

The test suite includes mock implementations for:
- **Orders Service** - Simulates order creation
- **Shipping Service** - Provides mock shipping rates
- **Redis Client** - In-memory Redis simulation

This comprehensive test suite ensures the Checkout service maintains high quality and reliability standards while demonstrating modern TypeScript/NestJS testing practices and enterprise-grade testing strategies.