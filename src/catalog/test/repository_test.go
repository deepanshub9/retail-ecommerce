package test

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"

	"github.com/aws-containers/retail-store-sample-app/catalog/config"
	"github.com/aws-containers/retail-store-sample-app/catalog/model"
	"github.com/aws-containers/retail-store-sample-app/catalog/repository"
)

type RepositoryTestSuite struct {
	suite.Suite
	db   *gorm.DB
	repo repository.CatalogRepository
}

func (suite *RepositoryTestSuite) SetupSuite() {
	// Use in-memory SQLite for testing
	db, err := gorm.Open(sqlite.Open("file::memory:?cache=shared"), &gorm.Config{})
	suite.Require().NoError(err)

	// Auto-migrate the schema
	err = db.AutoMigrate(&model.Product{}, &model.Tag{})
	suite.Require().NoError(err)

	suite.db = db

	// Create repository with in-memory config
	config := config.DatabaseConfiguration{
		Type: "in-memory",
	}
	
	repo, err := repository.NewRepository(config)
	suite.Require().NoError(err)
	suite.repo = repo
}

func (suite *RepositoryTestSuite) TestGetProducts() {
	ctx := context.Background()

	suite.Run("Get products with default parameters", func() {
		products, err := suite.repo.GetProducts([]string{}, "", 1, 10, ctx)
		
		suite.NoError(err)
		suite.NotEmpty(products)
		suite.LessOrEqual(len(products), 10)
	})

	suite.Run("Get products with pagination", func() {
		// Get first page
		page1, err := suite.repo.GetProducts([]string{}, "", 1, 3, ctx)
		suite.NoError(err)
		suite.LessOrEqual(len(page1), 3)

		// Get second page
		page2, err := suite.repo.GetProducts([]string{}, "", 2, 3, ctx)
		suite.NoError(err)

		// Pages should be different (if we have enough products)
		if len(page1) == 3 && len(page2) > 0 {
			suite.NotEqual(page1[0].ID, page2[0].ID)
		}
	})

	suite.Run("Get products with tag filter", func() {
		products, err := suite.repo.GetProducts([]string{"watch"}, "", 1, 10, ctx)
		
		suite.NoError(err)
		// Should return products with watch tag or empty if no such products
		for _, product := range products {
			suite.NotEmpty(product.ID)
			suite.NotEmpty(product.Name)
		}
	})

	suite.Run("Get products with price ordering", func() {
		productsAsc, err := suite.repo.GetProducts([]string{}, "price_asc", 1, 5, ctx)
		suite.NoError(err)

		productsDesc, err := suite.repo.GetProducts([]string{}, "price_desc", 1, 5, ctx)
		suite.NoError(err)

		// Verify ordering if we have multiple products
		if len(productsAsc) > 1 {
			for i := 1; i < len(productsAsc); i++ {
				suite.GreaterOrEqual(productsAsc[i].Price, productsAsc[i-1].Price)
			}
		}

		if len(productsDesc) > 1 {
			for i := 1; i < len(productsDesc); i++ {
				suite.LessOrEqual(productsDesc[i].Price, productsDesc[i-1].Price)
			}
		}
	})
}

func (suite *RepositoryTestSuite) TestGetProduct() {
	ctx := context.Background()

	suite.Run("Get existing product", func() {
		// First get a list of products to get a valid ID
		products, err := suite.repo.GetProducts([]string{}, "", 1, 1, ctx)
		suite.NoError(err)
		suite.NotEmpty(products)

		productID := products[0].ID
		product, err := suite.repo.GetProduct(productID, ctx)
		
		suite.NoError(err)
		suite.NotNil(product)
		suite.Equal(productID, product.ID)
		suite.NotEmpty(product.Name)
		suite.Greater(product.Price, 0)
	})

	suite.Run("Get non-existing product", func() {
		product, err := suite.repo.GetProduct("non-existing-id", ctx)
		
		suite.Error(err)
		suite.Nil(product)
	})
}

func (suite *RepositoryTestSuite) TestCountProducts() {
	ctx := context.Background()

	suite.Run("Count all products", func() {
		count, err := suite.repo.CountProducts([]string{}, ctx)
		
		suite.NoError(err)
		suite.Greater(count, 0)
	})

	suite.Run("Count products with tag filter", func() {
		totalCount, err := suite.repo.CountProducts([]string{}, ctx)
		suite.NoError(err)

		taggedCount, err := suite.repo.CountProducts([]string{"watch"}, ctx)
		suite.NoError(err)

		// Tagged count should be less than or equal to total count
		suite.LessOrEqual(taggedCount, totalCount)
	})

	suite.Run("Count products with non-existing tag", func() {
		count, err := suite.repo.CountProducts([]string{"non-existing-tag"}, ctx)
		
		suite.NoError(err)
		suite.Equal(0, count)
	})
}

func (suite *RepositoryTestSuite) TestGetTags() {
	ctx := context.Background()

	suite.Run("Get all tags", func() {
		tags, err := suite.repo.GetTags(ctx)
		
		suite.NoError(err)
		suite.NotEmpty(tags)

		// Verify tag structure
		for _, tag := range tags {
			suite.NotEmpty(tag.Name)
			suite.NotEmpty(tag.DisplayName)
		}
	})

	suite.Run("Verify tags are sorted by display name", func() {
		tags, err := suite.repo.GetTags(ctx)
		suite.NoError(err)

		if len(tags) > 1 {
			for i := 1; i < len(tags); i++ {
				suite.LessOrEqual(tags[i-1].DisplayName, tags[i].DisplayName)
			}
		}
	})
}

func (suite *RepositoryTestSuite) TestProductTagRelationship() {
	ctx := context.Background()

	suite.Run("Products have associated tags", func() {
		products, err := suite.repo.GetProducts([]string{}, "", 1, 5, ctx)
		suite.NoError(err)

		for _, product := range products {
			// Each product should have at least some basic structure
			suite.NotEmpty(product.ID)
			suite.NotEmpty(product.Name)
			suite.GreaterOrEqual(product.Price, 0)
			// Tags can be empty, but the field should exist
			suite.NotNil(product.Tags)
		}
	})
}

func TestRepositoryTestSuite(t *testing.T) {
	suite.Run(t, new(RepositoryTestSuite))
}

// Benchmark tests for performance
func BenchmarkGetProducts(b *testing.B) {
	config := config.DatabaseConfiguration{Type: "in-memory"}
	repo, _ := repository.NewRepository(config)
	ctx := context.Background()

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = repo.GetProducts([]string{}, "", 1, 10, ctx)
	}
}

func BenchmarkGetProductByID(b *testing.B) {
	config := config.DatabaseConfiguration{Type: "in-memory"}
	repo, _ := repository.NewRepository(config)
	ctx := context.Background()

	// Get a valid product ID first
	products, _ := repo.GetProducts([]string{}, "", 1, 1, ctx)
	if len(products) == 0 {
		b.Skip("No products available for benchmark")
	}
	productID := products[0].ID

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = repo.GetProduct(productID, ctx)
	}
}