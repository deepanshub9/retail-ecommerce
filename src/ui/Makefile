# UI Service Makefile

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
	@echo "Building UI service..."
	./mvnw clean compile

# Download dependencies
deps:
	@echo "Downloading dependencies..."
	./mvnw dependency:resolve

# Generate API clients
generate-clients:
	@echo "Generating API clients..."
	./mvnw kiota:generate

# Run all tests
test: test-unit test-integration

# Run unit tests
test-unit:
	@echo "Running unit tests..."
	./mvnw test -Dtest="!*IntegrationTest*"

# Run integration tests
test-integration:
	@echo "Running integration tests..."
	./mvnw test -Dtest="*IntegrationTest*"

# Run tests with coverage
test-coverage:
	@echo "Running tests with coverage..."
	./mvnw clean test jacoco:report
	@echo "Coverage report generated in target/site/jacoco/index.html"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	./mvnw clean

# Run the application locally
run:
	@echo "Running UI service locally..."
	./mvnw spring-boot:run

# Run with mock services
run-mock:
	@echo "Running UI service with mock services..."
	./mvnw spring-boot:run -Dspring.profiles.active=mock

# Run with development profile
run-dev:
	@echo "Running UI service in development mode..."
	./mvnw spring-boot:run -Dspring.profiles.active=dev

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build -t ui-service:latest .

# Run application in Docker
docker-run:
	@echo "Running UI service in Docker..."
	docker run -p 8080:8080 ui-service:latest

# Run with Docker Compose
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

# Full CI pipeline
ci: clean deps generate-clients analyze test-coverage package

# Development setup
dev-setup: deps generate-clients
	@echo "Development environment setup complete!"

# Load test with curl (requires service to be running)
load-test:
	@echo "Running basic load test..."
	@for i in {1..50}; do \
		curl -s http://localhost:8080/ > /dev/null & \
		curl -s http://localhost:8080/catalog > /dev/null & \
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
	curl -s http://localhost:8080/actuator/prometheus | head -20

# Watch for changes and restart
dev-watch:
	@echo "Starting development server with auto-reload..."
	./mvnw spring-boot:run -Dspring-boot.run.fork=false

# Run specific test class
test-class:
	@echo "Running specific test class: $(CLASS)"
	./mvnw test -Dtest=$(CLASS)

# Run performance tests
test-performance:
	@echo "Running performance tests..."
	./mvnw test -Dtest="*PerformanceTest*"

# Generate test reports
test-reports:
	@echo "Generating test reports..."
	./mvnw surefire-report:report
	@echo "Test reports generated in target/site/surefire-report.html"