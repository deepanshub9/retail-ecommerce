package test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"

	"github.com/aws-containers/retail-store-sample-app/catalog/model"
)

func TestCatalogEndpoints(t *testing.T) {
	gin.SetMode(gin.TestMode)

	t.Run("Get products list with default pagination", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/products", nil)

		assert.Equal(t, http.StatusOK, writer.Code)

		var response []model.Product
		err := json.Unmarshal(writer.Body.Bytes(), &response)
		assert.NoError(t, err)
		assert.GreaterOrEqual(t, len(response), 1)
		assert.LessOrEqual(t, len(response), 10) // Default page size
	})

	t.Run("Get products with pagination parameters", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/products?page=1&size=5", nil)

		assert.Equal(t, http.StatusOK, writer.Code)

		var response []model.Product
		err := json.Unmarshal(writer.Body.Bytes(), &response)
		assert.NoError(t, err)
		assert.LessOrEqual(t, len(response), 5)
	})

	t.Run("Get products with tag filter", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/products?tags=watch", nil)

		assert.Equal(t, http.StatusOK, writer.Code)

		var response []model.Product
		err := json.Unmarshal(writer.Body.Bytes(), &response)
		assert.NoError(t, err)
		// Should return products with watch tag
		for _, product := range response {
			assert.NotEmpty(t, product.ID)
			assert.NotEmpty(t, product.Name)
		}
	})

	t.Run("Get products with price ordering", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/products?order=price_asc&size=3", nil)

		assert.Equal(t, http.StatusOK, writer.Code)

		var response []model.Product
		err := json.Unmarshal(writer.Body.Bytes(), &response)
		assert.NoError(t, err)

		// Verify ascending price order
		if len(response) > 1 {
			for i := 1; i < len(response); i++ {
				assert.GreaterOrEqual(t, response[i].Price, response[i-1].Price)
			}
		}
	})

	t.Run("Get specific product by ID", func(t *testing.T) {
		// First get a product ID from the list
		listWriter := makeRequest("GET", "/catalog/products?size=1", nil)
		var products []model.Product
		json.Unmarshal(listWriter.Body.Bytes(), &products)

		if len(products) > 0 {
			productID := products[0].ID
			writer := makeRequest("GET", "/catalog/products/"+productID, nil)

			assert.Equal(t, http.StatusOK, writer.Code)

			var response model.Product
			err := json.Unmarshal(writer.Body.Bytes(), &response)
			assert.NoError(t, err)
			assert.Equal(t, productID, response.ID)
			assert.NotEmpty(t, response.Name)
			assert.Greater(t, response.Price, 0)
		}
	})

	t.Run("Get non-existing product returns 404", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/products/non-existing-id", nil)
		assert.Equal(t, http.StatusNotFound, writer.Code)
	})

	t.Run("Get catalog size", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/size", nil)

		assert.Equal(t, http.StatusOK, writer.Code)

		var response model.CatalogSizeResponse
		err := json.Unmarshal(writer.Body.Bytes(), &response)
		assert.NoError(t, err)
		assert.Greater(t, response.Size, 0)
	})

	t.Run("Get catalog size with tag filter", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/size?tags=watch", nil)

		assert.Equal(t, http.StatusOK, writer.Code)

		var response model.CatalogSizeResponse
		err := json.Unmarshal(writer.Body.Bytes(), &response)
		assert.NoError(t, err)
		assert.GreaterOrEqual(t, response.Size, 0)
	})

	t.Run("Get all tags", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/tags", nil)

		assert.Equal(t, http.StatusOK, writer.Code)

		var response []model.Tag
		err := json.Unmarshal(writer.Body.Bytes(), &response)
		assert.NoError(t, err)
		assert.Greater(t, len(response), 0)

		// Verify tag structure
		for _, tag := range response {
			assert.NotEmpty(t, tag.Name)
			assert.NotEmpty(t, tag.DisplayName)
		}
	})

	t.Run("Invalid pagination parameters", func(t *testing.T) {
		writer := makeRequest("GET", "/catalog/products?page=invalid&size=invalid", nil)
		assert.Equal(t, http.StatusBadRequest, writer.Code)
	})
}

func TestHealthEndpoint(t *testing.T) {
	writer := httptest.NewRecorder()
	req, _ := http.NewRequest("GET", "/health", nil)
	router().ServeHTTP(writer, req)

	assert.Equal(t, http.StatusOK, writer.Code)
	assert.Equal(t, "OK", writer.Body.String())
}

func TestTopologyEndpoint(t *testing.T) {
	writer := httptest.NewRecorder()
	req, _ := http.NewRequest("GET", "/topology", nil)
	router().ServeHTTP(writer, req)

	assert.Equal(t, http.StatusOK, writer.Code)

	var response map[string]string
	err := json.Unmarshal(writer.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Contains(t, response, "persistenceProvider")
	assert.Contains(t, response, "databaseEndpoint")
}
