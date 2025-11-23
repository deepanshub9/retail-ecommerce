package test

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"github.com/aws-containers/retail-store-sample-app/catalog/api"
	"github.com/aws-containers/retail-store-sample-app/catalog/model"
)

// MockRepository implements CatalogRepository interface for testing
type MockRepository struct {
	mock.Mock
}

func (m *MockRepository) GetProducts(tags []string, order string, pageNum, pageSize int, ctx context.Context) ([]model.Product, error) {
	args := m.Called(tags, order, pageNum, pageSize, ctx)
	return args.Get(0).([]model.Product), args.Error(1)
}

func (m *MockRepository) CountProducts(tags []string, ctx context.Context) (int, error) {
	args := m.Called(tags, ctx)
	return args.Int(0), args.Error(1)
}

func (m *MockRepository) GetProduct(id string, ctx context.Context) (*model.Product, error) {
	args := m.Called(id, ctx)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Product), args.Error(1)
}

func (m *MockRepository) GetTags(ctx context.Context) ([]model.Tag, error) {
	args := m.Called(ctx)
	return args.Get(0).([]model.Tag), args.Error(1)
}

func TestCatalogAPI_GetProducts(t *testing.T) {
	mockRepo := new(MockRepository)
	catalogAPI, _ := api.NewCatalogAPI(mockRepo)
	ctx := context.Background()

	// Test data
	expectedProducts := []model.Product{
		{ID: "1", Name: "Product 1", Price: 100},
		{ID: "2", Name: "Product 2", Price: 200},
	}

	t.Run("Get products successfully", func(t *testing.T) {
		mockRepo.On("GetProducts", []string{"electronics"}, "price_asc", 1, 10, ctx).Return(expectedProducts, nil)

		products, err := catalogAPI.GetProducts([]string{"electronics"}, "price_asc", 1, 10, ctx)

		assert.NoError(t, err)
		assert.Equal(t, 2, len(products))
		assert.Equal(t, "Product 1", products[0].Name)
		mockRepo.AssertExpectations(t)
	})

	t.Run("Get products with empty tags", func(t *testing.T) {
		mockRepo.On("GetProducts", []string{}, "", 1, 5, ctx).Return(expectedProducts, nil)

		products, err := catalogAPI.GetProducts([]string{}, "", 1, 5, ctx)

		assert.NoError(t, err)
		assert.Equal(t, 2, len(products))
		mockRepo.AssertExpectations(t)
	})
}

func TestCatalogAPI_GetProduct(t *testing.T) {
	mockRepo := new(MockRepository)
	catalogAPI, _ := api.NewCatalogAPI(mockRepo)
	ctx := context.Background()

	expectedProduct := &model.Product{
		ID:          "test-id",
		Name:        "Test Product",
		Description: "Test Description",
		Price:       150,
	}

	t.Run("Get existing product", func(t *testing.T) {
		mockRepo.On("GetProduct", "test-id", ctx).Return(expectedProduct, nil)

		product, err := catalogAPI.GetProduct("test-id", ctx)

		assert.NoError(t, err)
		assert.Equal(t, "Test Product", product.Name)
		assert.Equal(t, 150, product.Price)
		mockRepo.AssertExpectations(t)
	})

	t.Run("Get non-existing product", func(t *testing.T) {
		mockRepo.On("GetProduct", "non-existing", ctx).Return(nil, assert.AnError)

		product, err := catalogAPI.GetProduct("non-existing", ctx)

		assert.Error(t, err)
		assert.Nil(t, product)
		mockRepo.AssertExpectations(t)
	})
}

func TestCatalogAPI_GetTags(t *testing.T) {
	mockRepo := new(MockRepository)
	catalogAPI, _ := api.NewCatalogAPI(mockRepo)
	ctx := context.Background()

	expectedTags := []model.Tag{
		{Name: "electronics", DisplayName: "Electronics"},
		{Name: "clothing", DisplayName: "Clothing"},
	}

	t.Run("Get all tags", func(t *testing.T) {
		mockRepo.On("GetTags", ctx).Return(expectedTags, nil)

		tags, err := catalogAPI.GetTags(ctx)

		assert.NoError(t, err)
		assert.Equal(t, 2, len(tags))
		assert.Equal(t, "Electronics", tags[0].DisplayName)
		mockRepo.AssertExpectations(t)
	})
}

func TestCatalogAPI_GetSize(t *testing.T) {
	mockRepo := new(MockRepository)
	catalogAPI, _ := api.NewCatalogAPI(mockRepo)
	ctx := context.Background()

	t.Run("Get catalog size with tags", func(t *testing.T) {
		mockRepo.On("CountProducts", []string{"electronics"}, ctx).Return(25, nil)

		size, err := catalogAPI.GetSize([]string{"electronics"}, ctx)

		assert.NoError(t, err)
		assert.Equal(t, 25, size)
		mockRepo.AssertExpectations(t)
	})

	t.Run("Get total catalog size", func(t *testing.T) {
		mockRepo.On("CountProducts", []string{}, ctx).Return(100, nil)

		size, err := catalogAPI.GetSize([]string{}, ctx)

		assert.NoError(t, err)
		assert.Equal(t, 100, size)
		mockRepo.AssertExpectations(t)
	})
}