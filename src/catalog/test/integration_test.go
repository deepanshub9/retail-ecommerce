package test

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"github.com/aws-containers/retail-store-sample-app/catalog/model"
)

type IntegrationTestSuite struct {
	suite.Suite
	router *gin.Engine
}

func (suite *IntegrationTestSuite) SetupSuite() {
	gin.SetMode(gin.TestMode)
	suite.router = router()
}

func (suite *IntegrationTestSuite) TestCompleteWorkflow() {
	suite.Run("Complete catalog browsing workflow", func() {
		// Step 1: Get all available tags
		tagsResp := suite.makeRequest("GET", "/catalog/tags", nil)
		suite.Equal(http.StatusOK, tagsResp.Code)

		var tags []model.Tag
		err := json.Unmarshal(tagsResp.Body.Bytes(), &tags)
		suite.NoError(err)
		suite.NotEmpty(tags)

		// Step 2: Get catalog size
		sizeResp := suite.makeRequest("GET", "/catalog/size", nil)
		suite.Equal(http.StatusOK, sizeResp.Code)

		var sizeResponse model.CatalogSizeResponse
		err = json.Unmarshal(sizeResp.Body.Bytes(), &sizeResponse)
		suite.NoError(err)
		suite.Greater(sizeResponse.Size, 0)

		// Step 3: Browse products with pagination
		productsResp := suite.makeRequest("GET", "/catalog/products?page=1&size=5", nil)
		suite.Equal(http.StatusOK, productsResp.Code)

		var products []model.Product
		err = json.Unmarshal(productsResp.Body.Bytes(), &products)
		suite.NoError(err)
		suite.NotEmpty(products)
		suite.LessOrEqual(len(products), 5)

		// Step 4: Get details of first product
		if len(products) > 0 {
			productID := products[0].ID
			productResp := suite.makeRequest("GET", "/catalog/products/"+productID, nil)
			suite.Equal(http.StatusOK, productResp.Code)

			var product model.Product
			err = json.Unmarshal(productResp.Body.Bytes(), &product)
			suite.NoError(err)
			suite.Equal(productID, product.ID)
		}

		// Step 5: Filter products by tag (if tags exist)
		if len(tags) > 0 {
			tagName := tags[0].Name
			filteredResp := suite.makeRequest("GET", "/catalog/products?tags="+tagName, nil)
			suite.Equal(http.StatusOK, filteredResp.Code)

			var filteredProducts []model.Product
			err = json.Unmarshal(filteredResp.Body.Bytes(), &filteredProducts)
			suite.NoError(err)
		}
	})
}

func (suite *IntegrationTestSuite) TestPaginationConsistency() {
	suite.Run("Pagination should be consistent", func() {
		pageSize := 3
		
		// Get first page
		page1Resp := suite.makeRequest("GET", fmt.Sprintf("/catalog/products?page=1&size=%d", pageSize), nil)
		suite.Equal(http.StatusOK, page1Resp.Code)

		var page1Products []model.Product
		err := json.Unmarshal(page1Resp.Body.Bytes(), &page1Products)
		suite.NoError(err)

		// Get second page
		page2Resp := suite.makeRequest("GET", fmt.Sprintf("/catalog/products?page=2&size=%d", pageSize), nil)
		suite.Equal(http.StatusOK, page2Resp.Code)

		var page2Products []model.Product
		err = json.Unmarshal(page2Resp.Body.Bytes(), &page2Products)
		suite.NoError(err)

		// Verify no overlap between pages (if both pages have products)
		if len(page1Products) > 0 && len(page2Products) > 0 {
			page1IDs := make(map[string]bool)
			for _, product := range page1Products {
				page1IDs[product.ID] = true
			}

			for _, product := range page2Products {
				suite.False(page1IDs[product.ID], "Product %s appears in both pages", product.ID)
			}
		}
	})
}

func (suite *IntegrationTestSuite) TestSortingConsistency() {
	suite.Run("Price sorting should be consistent", func() {
		// Test ascending order
		ascResp := suite.makeRequest("GET", "/catalog/products?order=price_asc&size=5", nil)
		suite.Equal(http.StatusOK, ascResp.Code)

		var ascProducts []model.Product
		err := json.Unmarshal(ascResp.Body.Bytes(), &ascProducts)
		suite.NoError(err)

		// Verify ascending order
		if len(ascProducts) > 1 {
			for i := 1; i < len(ascProducts); i++ {
				suite.GreaterOrEqual(ascProducts[i].Price, ascProducts[i-1].Price,
					"Products not in ascending price order")
			}
		}

		// Test descending order
		descResp := suite.makeRequest("GET", "/catalog/products?order=price_desc&size=5", nil)
		suite.Equal(http.StatusOK, descResp.Code)

		var descProducts []model.Product
		err = json.Unmarshal(descResp.Body.Bytes(), &descProducts)
		suite.NoError(err)

		// Verify descending order
		if len(descProducts) > 1 {
			for i := 1; i < len(descProducts); i++ {
				suite.LessOrEqual(descProducts[i].Price, descProducts[i-1].Price,
					"Products not in descending price order")
			}
		}
	})
}

func (suite *IntegrationTestSuite) TestErrorHandling() {
	suite.Run("API should handle errors gracefully", func() {
		// Test invalid product ID
		resp := suite.makeRequest("GET", "/catalog/products/invalid-id-12345", nil)
		suite.Equal(http.StatusNotFound, resp.Code)

		// Test invalid pagination parameters
		resp = suite.makeRequest("GET", "/catalog/products?page=invalid", nil)
		suite.Equal(http.StatusBadRequest, resp.Code)

		resp = suite.makeRequest("GET", "/catalog/products?size=invalid", nil)
		suite.Equal(http.StatusBadRequest, resp.Code)

		// Test negative pagination parameters
		resp = suite.makeRequest("GET", "/catalog/products?page=-1", nil)
		suite.Equal(http.StatusBadRequest, resp.Code)
	})
}

func (suite *IntegrationTestSuite) TestResponseFormat() {
	suite.Run("API responses should have correct format", func() {
		// Test products response format
		resp := suite.makeRequest("GET", "/catalog/products?size=1", nil)
		suite.Equal(http.StatusOK, resp.Code)

		var products []model.Product
		err := json.Unmarshal(resp.Body.Bytes(), &products)
		suite.NoError(err)

		if len(products) > 0 {
			product := products[0]
			suite.NotEmpty(product.ID)
			suite.NotEmpty(product.Name)
			suite.GreaterOrEqual(product.Price, 0)
			suite.NotNil(product.Tags) // Tags array should exist even if empty
		}

		// Test tags response format
		resp = suite.makeRequest("GET", "/catalog/tags", nil)
		suite.Equal(http.StatusOK, resp.Code)

		var tags []model.Tag
		err = json.Unmarshal(resp.Body.Bytes(), &tags)
		suite.NoError(err)

		for _, tag := range tags {
			suite.NotEmpty(tag.Name)
			suite.NotEmpty(tag.DisplayName)
		}

		// Test size response format
		resp = suite.makeRequest("GET", "/catalog/size", nil)
		suite.Equal(http.StatusOK, resp.Code)

		var sizeResp model.CatalogSizeResponse
		err = json.Unmarshal(resp.Body.Bytes(), &sizeResp)
		suite.NoError(err)
		suite.GreaterOrEqual(sizeResp.Size, 0)
	})
}

func (suite *IntegrationTestSuite) TestPerformance() {
	suite.Run("API should respond within reasonable time", func() {
		endpoints := []string{
			"/catalog/products",
			"/catalog/tags",
			"/catalog/size",
			"/health",
		}

		for _, endpoint := range endpoints {
			start := time.Now()
			resp := suite.makeRequest("GET", endpoint, nil)
			duration := time.Since(start)

			suite.Equal(http.StatusOK, resp.Code)
			suite.Less(duration, 5*time.Second, "Endpoint %s took too long: %v", endpoint, duration)
		}
	})
}

func (suite *IntegrationTestSuite) makeRequest(method, url string, body interface{}) *httptest.ResponseRecorder {
	req, _ := http.NewRequest(method, url, nil)
	writer := httptest.NewRecorder()
	suite.router.ServeHTTP(writer, req)
	return writer
}

func TestIntegrationTestSuite(t *testing.T) {
	suite.Run(t, new(IntegrationTestSuite))
}

// Load testing
func TestConcurrentRequests(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := router()

	// Test concurrent requests to ensure thread safety
	concurrency := 10
	requests := 50

	results := make(chan int, concurrency*requests)

	for i := 0; i < concurrency; i++ {
		go func() {
			for j := 0; j < requests; j++ {
				resp := httptest.NewRecorder()
				req, _ := http.NewRequest("GET", "/catalog/products", nil)
				r.ServeHTTP(resp, req)
				results <- resp.Code
			}
		}()
	}

	// Collect results
	successCount := 0
	for i := 0; i < concurrency*requests; i++ {
		code := <-results
		if code == http.StatusOK {
			successCount++
		}
	}

	// All requests should succeed
	assert.Equal(t, concurrency*requests, successCount, "Not all concurrent requests succeeded")
}