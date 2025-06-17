package com.java.vidigal.code.test.config;

import com.java.vidigal.code.utilities.config.DeepLConfig;
import com.java.vidigal.code.utilities.config.DeepLConfigBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link DeepLConfigBuilder} class, verifying its ability to construct
 * {@link DeepLConfig} objects with specified settings, apply default values, and enforce
 * validation rules for configuration parameters. Tests cover valid configurations, default
 * values, and exception handling for invalid inputs.
 */
class DeepLConfigBuilderTest {

    /**
     * Tests the construction of a {@link DeepLConfig} object with all settings explicitly
     * specified using the {@link DeepLConfigBuilder}. Verifies that all configured values
     * are correctly set in the resulting configuration object.
     */
    @Test
    void shouldBuildValidConfigWithAllSettings() {
        DeepLConfig config = DeepLConfig.builder()
                .apiUrl("https://api-free.deepl.com/v2/translate")
                .authKey("your-api-key:fx")
                .connectionTimeout(5000)
                .socketTimeout(10000)
                .maxRequestsPerSecond(10)
                .maxRetries(3)
                .rateLimitCooldown(5000)
                .enableRetry(true)
                .enableCache(true)
                .enablePersistentCache(true, "/tmp/cache")
                .closedThreadAuto(true)
                .cacheTtlMillis(3600000)
                .cleanupIntervalMillis(1800000)
                .build();

        assertEquals("https://api-free.deepl.com/v2/translate", config.getApiUrl(), "API URL should match the specified value");
        assertEquals("your-api-key:fx", config.getAuthKey(), "Auth key should match the specified value");
        assertEquals(5000, config.getConnectionTimeout(), "Connection timeout should be 5000 ms");
        assertEquals(10000, config.getSocketTimeout(), "Socket timeout should be 10000 ms");
        assertEquals(10, config.getMaxRequestsPerSecond(), "Max requests per second should be 10");
        assertEquals(3, config.getMaxRetries(), "Max retries should be 3");
        assertEquals(5000, config.getRateLimitCooldown(), "Rate limit cooldown should be 5000 ms");
        assertTrue(config.isRetryEnabled(), "Retry should be enabled");
        assertTrue(config.isCacheEnabled(), "Cache should be enabled");
        assertTrue(config.isPersistentCacheEnabled(), "Persistent cache should be enabled");
        assertEquals("/tmp/cache", config.getPersistentCachePath(), "Persistent cache path should be '/tmp/cache'");
        assertTrue(config.isClosedThreadAuto(), "Closed thread auto should be enabled");
        assertEquals(3600000, config.getCacheTtlMillis(), "Cache TTL should be 3600000 ms");
        assertEquals(1800000, config.getCleanupIntervalMillis(), "Cleanup interval should be 1800000 ms");
    }

    /**
     * Tests the construction of a {@link DeepLConfig} object with only required settings
     * (API URL and auth key) specified. Verifies that default values are correctly applied
     * for all unspecified settings in the resulting configuration object.
     */
    @Test
    void shouldUseDefaultValuesWhenNotSpecified() {
        DeepLConfig config = DeepLConfig.builder()
                .apiUrl("https://api.deepl.com/v2/translate")
                .authKey("test-key")
                .build();

        assertEquals(5000, config.getConnectionTimeout(), "Default connection timeout should be 5000 ms");
        assertEquals(10000, config.getSocketTimeout(), "Default socket timeout should be 10000 ms");
        assertEquals(10, config.getMaxRequestsPerSecond(), "Default max requests per second should be 10");
        assertEquals(3, config.getMaxRetries(), "Default max retries should be 3");
        assertEquals(5000, config.getRateLimitCooldown(), "Default rate limit cooldown should be 5000 ms");
        assertTrue(config.isRetryEnabled(), "Default retry should be enabled");
        assertFalse(config.isCacheEnabled(), "Default cache should be disabled");
        assertFalse(config.isPersistentCacheEnabled(), "Default persistent cache should be disabled");
        assertFalse(config.isClosedThreadAuto(), "Default closed thread auto should be disabled");
        assertEquals(3600000, config.getCacheTtlMillis(), "Default cache TTL should be 3600000 ms");
        assertEquals(1800000, config.getCleanupIntervalMillis(), "Default cleanup interval should be 1800000 ms");
    }

    /**
     * Tests that setting a null API URL via the {@link DeepLConfigBuilder#apiUrl} method
     * throws an {@link IllegalArgumentException} with the appropriate error message.
     */
    @Test
    void shouldThrowExceptionForNullApiUrl() {
        DeepLConfigBuilder builder = DeepLConfig.builder().authKey("test-key");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.apiUrl(null),
                "Expected IllegalArgumentException for null API URL"
        );
        assertEquals("API URL cannot be null or blank", exception.getMessage(), "Exception message should indicate null or blank API URL");
    }

    /**
     * Tests that setting a blank API URL via the {@link DeepLConfigBuilder#apiUrl} method
     * throws an {@link IllegalArgumentException} with the appropriate error message.
     */
    @Test
    void shouldThrowExceptionForBlankApiUrl() {
        DeepLConfigBuilder builder = DeepLConfig.builder().authKey("test-key");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.apiUrl(" "),
                "Expected IllegalArgumentException for blank API URL"
        );
        assertEquals("API URL cannot be null or blank", exception.getMessage(), "Exception message should indicate null or blank API URL");
    }


    /**
     * Tests that setting an invalid connection timeout (below the allowed range) via the
     * {@link DeepLConfigBuilder#connectionTimeout} method throws an
     * {@link IllegalArgumentException} with an error message indicating the valid range.
     */
    @Test
    void shouldThrowExceptionForInvalidConnectionTimeout() {
        DeepLConfigBuilder builder = DeepLConfig.builder().apiUrl("https://api.deepl.com/v2/translate").authKey("test-key");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.connectionTimeout(50),
                "Expected IllegalArgumentException for invalid connection timeout"
        );
        assertTrue(exception.getMessage().contains("Connection timeout must be between"), "Exception message should indicate valid range for connection timeout");
    }

    /**
     * Tests that setting an invalid socket timeout (below the allowed range) via the
     * {@link DeepLConfigBuilder#socketTimeout} method throws an
     * {@link IllegalArgumentException} with an error message indicating the valid range.
     */
    @Test
    void shouldThrowExceptionForInvalidSocketTimeout() {
        DeepLConfigBuilder builder = DeepLConfig.builder().apiUrl("https://api.deepl.com/v2/translate").authKey("test-key");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.socketTimeout(50),
                "Expected IllegalArgumentException for invalid socket timeout"
        );
        assertTrue(exception.getMessage().contains("Socket timeout must be between"), "Exception message should indicate valid range for socket timeout");
    }

    /**
     * Tests that setting an invalid maximum requests per second (zero or negative) via the
     * {@link DeepLConfigBuilder#maxRequestsPerSecond} method throws an
     * {@link IllegalArgumentException} with an error message indicating the valid range.
     */
    @Test
    void shouldThrowExceptionForInvalidMaxRequestsPerSecond() {
        DeepLConfigBuilder builder = DeepLConfig.builder().apiUrl("https://api.deepl.com/v2/translate").authKey("test-key");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.maxRequestsPerSecond(0),
                "Expected IllegalArgumentException for invalid max requests per second"
        );
        assertTrue(exception.getMessage().contains("Max requests per second must be between"), "Exception message should indicate valid range for max requests per second");
    }


    /**
     * Tests that setting an invalid cleanup interval (below 1000 ms) via the
     * {@link DeepLConfigBuilder#cleanupIntervalMillis} method throws an
     * {@link IllegalArgumentException} with the appropriate error message.
     */
    @Test
    void shouldThrowExceptionForInvalidCleanupInterval() {
        DeepLConfigBuilder builder = DeepLConfig.builder().apiUrl("https://api.deepl.com/v2/translate").authKey("test-key");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.cleanupIntervalMillis(500),
                "Expected IllegalArgumentException for invalid cleanup interval"
        );
        assertEquals("Cleanup interval must be at least 1000 ms", exception.getMessage(), "Exception message should indicate minimum cleanup interval");
    }

    /**
     * Tests that enabling persistent cache without providing a valid path via the
     * {@link DeepLConfigBuilder#enablePersistentCache} method throws an
     * {@link IllegalArgumentException} with the appropriate error message.
     */
    @Test
    void shouldThrowExceptionForPersistentCacheWithoutPath() {
        DeepLConfigBuilder builder = DeepLConfig.builder().apiUrl("https://api.deepl.com/v2/translate").authKey("test-key");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.enablePersistentCache(true, null),
                "Expected IllegalArgumentException for null persistent cache path"
        );
        assertEquals("Persistent cache path cannot be null or empty", exception.getMessage(), "Exception message should indicate null or empty cache path");
    }
}