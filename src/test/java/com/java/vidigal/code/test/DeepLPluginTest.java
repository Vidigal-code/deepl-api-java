package com.java.vidigal.code.test;

import com.java.vidigal.code.DeepLPlugin;
import com.java.vidigal.code.client.DeepLClient;
import com.java.vidigal.code.exception.DeepLException;
import com.java.vidigal.code.language.LanguageRegistry;
import com.java.vidigal.code.request.Translation;
import com.java.vidigal.code.request.TranslationRequest;
import com.java.vidigal.code.request.TranslationResponse;
import com.java.vidigal.code.utilities.cache.TranslationCache;
import com.java.vidigal.code.utilities.config.DeepLConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link DeepLPlugin} class, verifying its functionality as a high-level interface
 * for interacting with the DeepL translation API. Tests cover language validation, synchronous and
 * asynchronous translations, batch processing, cache operations, and monitoring stats using a mocked
 * {@link DeepLClient} and {@link LanguageRegistry}.
 */
class DeepLPluginTest {

    @Mock
    private DeepLClient deepLClient;

    @Mock
    private LanguageRegistry languageRegistry;

    private DeepLPlugin plugin;
    private DeepLConfig config;
    private AutoCloseable mocks;

    /**
     * Sets up the test environment before each test. Initializes Mockito mocks, configures a
     * {@link DeepLConfig} with caching enabled, creates a {@link DeepLPlugin} instance with mocked
     * dependencies, and defines mock behavior for translation requests and language validation.
     */
    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        config = DeepLConfig.builder()
                .apiUrl("https://api-free.deepl.com/v2/translate")
                .authKey("YOUR_API_KEY:fx")
                .enableCache(true)
                .cacheTtlMillis(3_600_000)
                .build();

        // Mock LanguageRegistry to support specific languages
        when(languageRegistry.isSupported("ES")).thenReturn(true);
        when(languageRegistry.isSupported("EN")).thenReturn(true);
        when(languageRegistry.isSupported("XX")).thenReturn(false);

        plugin = new DeepLPlugin(config, languageRegistry) {
            @Override
            public DeepLClient getClient() {
                return deepLClient;
            }
        };

        TranslationResponse response = new TranslationResponse(List.of(new Translation("Hola", "EN")));
        when(deepLClient.translate(any(TranslationRequest.class))).thenReturn(response);
        when(deepLClient.translateAsync(any(TranslationRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    /**
     * Cleans up after each test by shutting down the {@link DeepLPlugin} instance and releasing
     * Mockito mocks to prevent resource leaks.
     *
     * @throws Exception if an error occurs during cleanup.
     */
    @AfterEach
    void tearDown() throws Exception {
        plugin.shutdown();
        mocks.close();
    }

    /**
     * Tests that translating text with an unsupported target language throws a
     * {@link DeepLException} with the appropriate error message. Verifies that the plugin
     * correctly validates the target language using the mocked {@link LanguageRegistry}.
     */
    @Test
    void shouldThrowExceptionForUnsupportedTargetLang() {
        DeepLException exception = assertThrows(
                DeepLException.class,
                () -> plugin.translateText("Hello", "XX"),
                "Expected DeepLException for unsupported target language"
        );
        assertEquals("Unsupported target language: XX", exception.getMessage(), "Exception message should indicate unsupported target language");
        verify(languageRegistry).isSupported("XX");
        verifyNoInteractions(deepLClient);
    }

    /**
     * Tests that translating text with an unsupported source language throws a
     * {@link DeepLException} with the appropriate error message. Verifies that the plugin
     * correctly validates the source language using the mocked {@link LanguageRegistry}.
     */
    @Test
    void shouldThrowExceptionForUnsupportedSourceLang() {
        DeepLException exception = assertThrows(
                DeepLException.class,
                () -> plugin.translateText("Hello", "ES", "XX"),
                "Expected DeepLException for unsupported source language"
        );
        assertEquals("Unsupported source language: XX", exception.getMessage(), "Exception message should indicate unsupported source language");
        verify(languageRegistry).isSupported("ES");
        verify(languageRegistry).isSupported("XX");
        verifyNoInteractions(deepLClient);
    }


    /**
     * Tests that retrieving the cache returns an empty list when the client is not a
     * {@link com.java.vidigal.code.client.DeepLClientImpl} or caching is disabled.
     */
    @Test
    void shouldReturnEmptyCacheWhenCachingDisabled() {
        DeepLConfig noCacheConfig = DeepLConfig.builder()
                .apiUrl("https://api-free.deepl.com/v2/translate")
                .authKey("your-api-key:fx")
                .enableCache(false)
                .build();
        DeepLPlugin pluginNoCache = new DeepLPlugin(noCacheConfig, languageRegistry);
        List<TranslationCache.CacheEntry> cache = pluginNoCache.getAllCache();
        assertTrue(cache.isEmpty(), "Cache should be empty when caching is disabled");
    }

    /**
     * Tests that clearing the cache does not throw an exception when the client does not
     * support caching, and no interactions occur with the mocked client.
     */
    @Test
    void shouldClearCacheWithoutException() {
        plugin.clearCache();
        verifyNoInteractions(deepLClient); // No cache to clear since client is mocked
    }


}