# Catalog Service Makefile

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
	@echo "Building catalog service..."
	go build -o bin/catalog main.go

# Download dependencies
deps:
	@echo "Downloading dependencies..."
	go mod download
	go mod tidy

# Run all tests
test: test-unit test-integration

# Run unit tests
test-unit:
	@echo "Running unit tests..."
	go test -v ./test -run "Test.*API|Test.*Config|Test.*Repository" -short

# Run integration tests
test-integration:
	@echo "Running integration tests..."
	go test -v ./test -run "Test.*Integration|Test.*Workflow|Test.*Concurrent"

# Run tests with coverage
test-coverage:
	@echo "Running tests with coverage..."
	go test -v ./test -coverprofile=coverage.out -covermode=atomic
	go tool cover -html=coverage.out -o coverage.html
	@echo "Coverage report generated: coverage.html"

# Run chaos engineering tests
test-chaos:
	@echo "Running chaos engineering tests..."
	go test -v ./test -run "Test.*Chaos"

# Run benchmark tests
test-benchmark:
	@echo "Running benchmark tests..."
	go test -v ./test -bench=. -benchmem

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	rm -f bin/catalog
	rm -f coverage.out
	rm -f coverage.html
	go clean -testcache

# Run the application locally
run:
	@echo "Running catalog service locally..."
	go run main.go

# Run with in-memory database
run-memory:
	@echo "Running catalog service with in-memory database..."
	RETAIL_CATALOG_PERSISTENCE_PROVIDER=in-memory go run main.go

# Run with MySQL (requires MySQL to be running)
run-mysql:
	@echo "Running catalog service with MySQL..."
	RETAIL_CATALOG_PERSISTENCE_PROVIDER=mysql \
	RETAIL_CATALOG_PERSISTENCE_ENDPOINT=localhost:3306 \
	RETAIL_CATALOG_PERSISTENCE_DB_NAME=catalogdb \
	RETAIL_CATALOG_PERSISTENCE_USER=catalog_user \
	RETAIL_CATALOG_PERSISTENCE_PASSWORD=password123 \
	go run main.go

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build -t catalog-service:latest .

# Run application in Docker
docker-run:
	@echo "Running catalog service in Docker..."
	docker run -p 8080:8080 -e RETAIL_CATALOG_PERSISTENCE_PROVIDER=in-memory catalog-service:latest

# Run with Docker Compose (includes MySQL)
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
	golangci-lint run

# Format the code
fmt:
	@echo "Formatting code..."
	go fmt ./...

# Vet the code
vet:
	@echo "Vetting code..."
	go vet ./...

# Security scan
security:
	@echo "Running security scan..."
	gosec ./...

# Generate API documentation
docs:
	@echo "Generating API documentation..."
	swag init -g main.go

# Install development tools
install-tools:
	@echo "Installing development tools..."
	go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest
	go install github.com/securecodewarrior/gosec/v2/cmd/gosec@latest
	go install github.com/swaggo/swag/cmd/swag@latest

# Run quality checks
quality: fmt vet lint security

# Full CI pipeline
ci: deps quality test-coverage

# Development setup
dev-setup: install-tools deps
	@echo "Development environment setup complete!"

# Load test with curl (requires service to be running)
load-test:
	@echo "Running basic load test..."
	@for i in {1..100}; do \
		curl -s http://localhost:8080/catalog/products > /dev/null & \
	done; \
	wait
	@echo "Load test completed"

# Health check
health-check:
	@echo "Checking service health..."
	curl -f http://localhost:8080/health || exit 1
	@echo "Service is healthy"