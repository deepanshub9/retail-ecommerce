package test

import (
	"context"
	"os"
	"testing"

	"github.com/sethvargo/go-envconfig/pkg/envconfig"
	"github.com/stretchr/testify/assert"

	"github.com/aws-containers/retail-store-sample-app/catalog/config"
)

func TestConfigurationLoading(t *testing.T) {
	t.Run("Load default configuration", func(t *testing.T) {
		ctx := context.Background()
		var cfg config.AppConfiguration

		err := envconfig.Process(ctx, &cfg)
		assert.NoError(t, err)

		// Test default values
		assert.Equal(t, 8080, cfg.Port)
		assert.Equal(t, "in-memory", cfg.Database.Type)
		assert.Equal(t, "catalogdb", cfg.Database.Name)
		assert.Equal(t, "catalog_user", cfg.Database.User)
		assert.Equal(t, 5, cfg.Database.ConnectTimeout)
	})

	t.Run("Load configuration from environment variables", func(t *testing.T) {
		// Set environment variables
		os.Setenv("PORT", "9090")
		os.Setenv("RETAIL_CATALOG_PERSISTENCE_PROVIDER", "mysql")
		os.Setenv("RETAIL_CATALOG_PERSISTENCE_ENDPOINT", "localhost:3306")
		os.Setenv("RETAIL_CATALOG_PERSISTENCE_DB_NAME", "testdb")
		os.Setenv("RETAIL_CATALOG_PERSISTENCE_USER", "testuser")
		os.Setenv("RETAIL_CATALOG_PERSISTENCE_PASSWORD", "testpass")
		os.Setenv("RETAIL_CATALOG_PERSISTENCE_CONNECT_TIMEOUT", "10")

		defer func() {
			// Clean up environment variables
			os.Unsetenv("PORT")
			os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_PROVIDER")
			os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_ENDPOINT")
			os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_DB_NAME")
			os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_USER")
			os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_PASSWORD")
			os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_CONNECT_TIMEOUT")
		}()

		ctx := context.Background()
		var cfg config.AppConfiguration

		err := envconfig.Process(ctx, &cfg)
		assert.NoError(t, err)

		// Test environment variable values
		assert.Equal(t, 9090, cfg.Port)
		assert.Equal(t, "mysql", cfg.Database.Type)
		assert.Equal(t, "localhost:3306", cfg.Database.Endpoint)
		assert.Equal(t, "testdb", cfg.Database.Name)
		assert.Equal(t, "testuser", cfg.Database.User)
		assert.Equal(t, "testpass", cfg.Database.Password)
		assert.Equal(t, 10, cfg.Database.ConnectTimeout)
	})

	t.Run("Partial environment variable override", func(t *testing.T) {
		// Set only some environment variables
		os.Setenv("PORT", "7070")
		os.Setenv("RETAIL_CATALOG_PERSISTENCE_PROVIDER", "mysql")

		defer func() {
			os.Unsetenv("PORT")
			os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_PROVIDER")
		}()

		ctx := context.Background()
		var cfg config.AppConfiguration

		err := envconfig.Process(ctx, &cfg)
		assert.NoError(t, err)

		// Test mixed values (some from env, some defaults)
		assert.Equal(t, 7070, cfg.Port)                    // From env
		assert.Equal(t, "mysql", cfg.Database.Type)        // From env
		assert.Equal(t, "catalogdb", cfg.Database.Name)    // Default
		assert.Equal(t, "catalog_user", cfg.Database.User) // Default
		assert.Equal(t, 5, cfg.Database.ConnectTimeout)    // Default
	})
}

func TestDatabaseConfiguration(t *testing.T) {
	t.Run("In-memory database configuration", func(t *testing.T) {
		cfg := config.DatabaseConfiguration{
			Type:           "in-memory",
			Name:           "catalogdb",
			User:           "catalog_user",
			ConnectTimeout: 5,
		}

		assert.Equal(t, "in-memory", cfg.Type)
		assert.Empty(t, cfg.Endpoint) // Should be empty for in-memory
		assert.Empty(t, cfg.Password) // Should be empty for in-memory
	})

	t.Run("MySQL database configuration", func(t *testing.T) {
		cfg := config.DatabaseConfiguration{
			Type:           "mysql",
			Endpoint:       "localhost:3306",
			Name:           "catalogdb",
			User:           "catalog_user",
			Password:       "password123",
			ConnectTimeout: 10,
		}

		assert.Equal(t, "mysql", cfg.Type)
		assert.Equal(t, "localhost:3306", cfg.Endpoint)
		assert.Equal(t, "catalogdb", cfg.Name)
		assert.Equal(t, "catalog_user", cfg.User)
		assert.Equal(t, "password123", cfg.Password)
		assert.Equal(t, 10, cfg.ConnectTimeout)
	})
}

func TestConfigurationValidation(t *testing.T) {
	t.Run("Valid port numbers", func(t *testing.T) {
		validPorts := []string{"80", "8080", "9000", "3000"}

		for _, port := range validPorts {
			os.Setenv("PORT", port)
			defer os.Unsetenv("PORT")

			ctx := context.Background()
			var cfg config.AppConfiguration

			err := envconfig.Process(ctx, &cfg)
			assert.NoError(t, err)
			assert.Greater(t, cfg.Port, 0)
			assert.Less(t, cfg.Port, 65536)
		}
	})

	t.Run("Valid database types", func(t *testing.T) {
		validTypes := []string{"in-memory", "mysql"}

		for _, dbType := range validTypes {
			os.Setenv("RETAIL_CATALOG_PERSISTENCE_PROVIDER", dbType)
			defer os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_PROVIDER")

			ctx := context.Background()
			var cfg config.AppConfiguration

			err := envconfig.Process(ctx, &cfg)
			assert.NoError(t, err)
			assert.Contains(t, validTypes, cfg.Database.Type)
		}
	})

	t.Run("Valid timeout values", func(t *testing.T) {
		validTimeouts := []string{"1", "5", "10", "30"}

		for _, timeout := range validTimeouts {
			os.Setenv("RETAIL_CATALOG_PERSISTENCE_CONNECT_TIMEOUT", timeout)
			defer os.Unsetenv("RETAIL_CATALOG_PERSISTENCE_CONNECT_TIMEOUT")

			ctx := context.Background()
			var cfg config.AppConfiguration

			err := envconfig.Process(ctx, &cfg)
			assert.NoError(t, err)
			assert.Greater(t, cfg.Database.ConnectTimeout, 0)
		}
	})
}

func TestEnvironmentVariableNames(t *testing.T) {
	t.Run("All environment variable names should be correct", func(t *testing.T) {
		// This test ensures we're using the correct environment variable names
		// as defined in the config struct tags

		expectedEnvVars := map[string]string{
			"PORT":                                        "8080",
			"RETAIL_CATALOG_PERSISTENCE_PROVIDER":        "mysql",
			"RETAIL_CATALOG_PERSISTENCE_ENDPOINT":        "localhost:3306",
			"RETAIL_CATALOG_PERSISTENCE_DB_NAME":         "testdb",
			"RETAIL_CATALOG_PERSISTENCE_USER":            "testuser",
			"RETAIL_CATALOG_PERSISTENCE_PASSWORD":        "testpass",
			"RETAIL_CATALOG_PERSISTENCE_CONNECT_TIMEOUT": "15",
		}

		// Set all environment variables
		for key, value := range expectedEnvVars {
			os.Setenv(key, value)
		}

		defer func() {
			// Clean up all environment variables
			for key := range expectedEnvVars {
				os.Unsetenv(key)
			}
		}()

		ctx := context.Background()
		var cfg config.AppConfiguration

		err := envconfig.Process(ctx, &cfg)
		assert.NoError(t, err)

		// Verify all values were loaded correctly
		assert.Equal(t, 8080, cfg.Port)
		assert.Equal(t, "mysql", cfg.Database.Type)
		assert.Equal(t, "localhost:3306", cfg.Database.Endpoint)
		assert.Equal(t, "testdb", cfg.Database.Name)
		assert.Equal(t, "testuser", cfg.Database.User)
		assert.Equal(t, "testpass", cfg.Database.Password)
		assert.Equal(t, 15, cfg.Database.ConnectTimeout)
	})
}