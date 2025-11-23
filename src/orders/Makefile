# Orders Service Makefile

.PHONY: help build test test-unit test-integration test-coverage clean run docker-build docker-run deps

# Default target
help:
	@echo "Available targets:"
	@echo "  build           - Build the application"
	@echo "  test            - Run all tests"
	@echo "  test-unit       - Run unit tests only"
	@echo "  test-integration - Run integration tests only"
	@echo "  test-coverage   - Run tests with coverage report"
	@echo "  clean           - Clean build artifacts"
	@echo "  run             - Run the application locally"
	@echo "  docker-build    - Build Docker image"
	@echo "  docker-run      - Run application in Docker"
	@echo "  deps            - Download dependencies"

# Build the application
build:
	@echo "Building orders service..."
	./mvnw clean compile

# Download dependencies
deps:
	@echo "Downloading dependencies..."
	./mvnw dependency:resolve

# Run all tests
test: test-unit test-integration

# Run unit tests
test-unit:
	@echo "Running unit tests..."
	./mvnw test -Dtest="!*IntegrationTest*,!*PostgresTest*"

# Run integration tests
test-integration:
	@echo "Running integration tests..."
	./mvnw test -Dtest="*IntegrationTest*,*PostgresTest*"

# Run tests with coverage
test-coverage:
	@echo "Running tests with coverage..."
	./mvnw clean test jacoco:report
	@echo "Coverage report generated in target/site/jacoco/index.html"

# Run chaos engineering tests
test-chaos:
	@echo "Running chaos engineering tests..."
	./mvnw test -Dtest="*ChaosTest*"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	./mvnw clean

# Run the application locally
run:
	@echo "Running orders service locally..."
	./mvnw spring-boot:run

# Run with PostgreSQL (requires PostgreSQL to be running)
run-postgres:
	@echo "Running orders service with PostgreSQL..."
	RETAIL_ORDERS_PERSISTENCE_PROVIDER=postgres \
	RETAIL_ORDERS_PERSISTENCE_ENDPOINT=localhost:5432 \
	RETAIL_ORDERS_PERSISTENCE_USERNAME=orders_user \
	RETAIL_ORDERS_PERSISTENCE_PASSWORD=password123 \
	RETAIL_ORDERS_PERSISTENCE_NAME=ordersdb \
	./mvnw spring-boot:run

# Run with RabbitMQ messaging
run-rabbitmq:
	@echo "Running orders service with RabbitMQ..."
	RETAIL_ORDERS_MESSAGING_PROVIDER=rabbitmq \
	RETAIL_ORDERS_MESSAGING_RABBITMQ_HOST=localhost \
	RETAIL_ORDERS_MESSAGING_RABBITMQ_PORT=5672 \
	RETAIL_ORDERS_MESSAGING_RABBITMQ_USERNAME=guest \
	RETAIL_ORDERS_MESSAGING_RABBITMQ_PASSWORD=guest \
	./mvnw spring-boot:run

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build -t orders-service:latest .

# Run application in Docker
docker-run:
	@echo "Running orders service in Docker..."
	docker run -p 8080:8080 -e RETAIL_ORDERS_PERSISTENCE_PROVIDER=h2 orders-service:latest

# Run with Docker Compose (includes PostgreSQL and RabbitMQ)
docker-compose-up:
	@echo "Starting services with Docker Compose..."
	docker-compose up -d

# Stop Docker Compose services
docker-compose-down:
	@echo "Stopping Docker Compose services..."
	docker-compose down

# Package the application
package:
	@echo "Packaging application..."
	./mvnw clean package -DskipTests

# Install the application
install:
	@echo "Installing application..."
	./mvnw clean install

# Run static analysis
analyze:
	@echo "Running static analysis..."
	./mvnw checkstyle:check spotbugs:check

# Generate API documentation
docs:
	@echo "Generating API documentation..."
	./mvnw spring-boot:start springdoc-openapi:generate spring-boot:stop

# Run performance tests
test-performance:
	@echo "Running performance tests..."
	./mvnw test -Dtest="*PerformanceTest*"

# Database migration
db-migrate:
	@echo "Running database migrations..."
	./mvnw flyway:migrate

# Database clean
db-clean:
	@echo "Cleaning database..."
	./mvnw flyway:clean

# Full CI pipeline
ci: clean deps analyze test-coverage package

# Development setup
dev-setup:
	@echo "Setting up development environment..."
	./mvnw dependency:resolve
	@echo "Development environment setup complete!"

# Load test with curl (requires service to be running)
load-test:
	@echo "Running basic load test..."
	@for i in {1..50}; do \
		curl -s -X POST http://localhost:8080/orders \
		-H "Content-Type: application/json" \
		-d '{"items":[{"productId":"test","quantity":1,"unitPrice":10.00}],"shippingAddress":{"firstName":"Test","lastName":"User","email":"test@example.com","address1":"Test St","city":"Test","zip":"12345","state":"TS"}}' > /dev/null & \
	done; \
	wait
	@echo "Load test completed"

# Health check
health-check:
	@echo "Checking service health..."
	curl -f http://localhost:8080/actuator/health || exit 1
	@echo "Service is healthy"

# Metrics check
metrics:
	@echo "Fetching service metrics..."
	curl -s http://localhost:8080/actuator/metrics | head -20