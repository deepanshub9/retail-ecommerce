# Checkout Service Makefile

.PHONY: help build test test-unit test-integration test-e2e test-coverage clean run docker-build docker-run deps

# Default target
help:
	@echo "Available targets:"
	@echo "  build           - Build the application"
	@echo "  test            - Run all tests"
	@echo "  test-unit       - Run unit tests only"
	@echo "  test-integration - Run integration tests only"
	@echo "  test-e2e        - Run end-to-end tests only"
	@echo "  test-coverage   - Run tests with coverage report"
	@echo "  clean           - Clean build artifacts"
	@echo "  run             - Run the application locally"
	@echo "  docker-build    - Build Docker image"
	@echo "  docker-run      - Run application in Docker"
	@echo "  deps            - Install dependencies"

# Install dependencies
deps:
	@echo "Installing dependencies..."
	yarn install

# Build the application
build:
	@echo "Building checkout service..."
	yarn build

# Run all tests
test: test-unit test-e2e

# Run unit tests
test-unit:
	@echo "Running unit tests..."
	yarn test --testPathPattern="src/.*\.spec\.ts$$"

# Run integration tests
test-integration:
	@echo "Running integration tests..."
	yarn test --testPathPattern="test/.*\.spec\.ts$$"

# Run end-to-end tests
test-e2e:
	@echo "Running end-to-end tests..."
	yarn test:integration

# Run tests with coverage
test-coverage:
	@echo "Running tests with coverage..."
	yarn test:cov
	@echo "Coverage report generated in coverage/ directory"

# Run chaos engineering tests
test-chaos:
	@echo "Running chaos engineering tests..."
	yarn test --testPathPattern="chaos.*\.spec\.ts$$"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	rm -rf dist/
	rm -rf coverage/
	rm -rf node_modules/.cache/

# Run the application locally
run:
	@echo "Running checkout service locally..."
	yarn serve:dev

# Run with Redis
run-redis:
	@echo "Running checkout service with Redis..."
	PERSISTENCE_PROVIDER=redis \
	REDIS_URL=redis://localhost:6379 \
	yarn serve:dev

# Run with in-memory storage
run-memory:
	@echo "Running checkout service with in-memory storage..."
	PERSISTENCE_PROVIDER=in-memory \
	yarn serve:dev

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build -t checkout-service:latest .

# Run application in Docker
docker-run:
	@echo "Running checkout service in Docker..."
	docker run -p 8080:8080 -e PERSISTENCE_PROVIDER=in-memory checkout-service:latest

# Run with Docker Compose (includes Redis)
docker-compose-up:
	@echo "Starting services with Docker Compose..."
	docker-compose up -d

# Stop Docker Compose services
docker-compose-down:
	@echo "Stopping Docker Compose services..."
	docker-compose down

# Lint the code
lint:
	@echo "Running linter..."
	yarn lint

# Fix linting issues
lint-fix:
	@echo "Fixing linting issues..."
	yarn lint:fix

# Format the code
format:
	@echo "Formatting code..."
	yarn format

# Type check
type-check:
	@echo "Running TypeScript type check..."
	npx tsc --noEmit

# Generate API documentation
docs:
	@echo "Starting application for API documentation..."
	@echo "Visit http://localhost:8080/api for Swagger documentation"
	yarn serve:dev

# Run performance tests
test-performance:
	@echo "Running performance tests..."
	yarn test --testPathPattern="performance.*\.spec\.ts$$"

# Full CI pipeline
ci: deps lint type-check test-coverage build

# Development setup
dev-setup: deps
	@echo "Development environment setup complete!"
	@echo "Run 'make run' to start the development server"

# Load test with curl (requires service to be running)
load-test:
	@echo "Running basic load test..."
	@for i in {1..50}; do \
		curl -s -X POST http://localhost:8080/checkout/test-$$i/update \
		-H "Content-Type: application/json" \
		-d '{"items":[{"id":"test","name":"Test Product","quantity":1,"price":10.00}],"shippingAddress":null,"deliveryOptionToken":null}' > /dev/null & \
	done; \
	wait
	@echo "Load test completed"

# Health check
health-check:
	@echo "Checking service health..."
	curl -f http://localhost:8080/health || exit 1
	@echo "Service is healthy"

# Watch mode for development
dev:
	@echo "Starting development server with watch mode..."
	yarn serve:dev

# Debug mode
debug:
	@echo "Starting service in debug mode..."
	yarn serve:debug

# Production build and serve
prod:
	@echo "Building and serving production version..."
	yarn build
	yarn serve:prod

# Install global dependencies
install-global:
	@echo "Installing global dependencies..."
	npm install -g @nestjs/cli

# Update dependencies
update-deps:
	@echo "Updating dependencies..."
	yarn upgrade

# Security audit
audit:
	@echo "Running security audit..."
	yarn audit

# Bundle analysis
analyze:
	@echo "Analyzing bundle size..."
	npx webpack-bundle-analyzer dist/main.js