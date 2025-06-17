package com.java.vidigal.code.test.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.vidigal.code.client.DeepLClientImpl;
import com.java.vidigal.code.exception.DeepLException;
import com.java.vidigal.code.request.Translation;
import com.java.vidigal.code.request.TranslationRequest;
import com.java.vidigal.code.request.TranslationResponse;
import com.java.vidigal.code.utilities.config.DeepLConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link DeepLClientImpl} class, verifying its functionality for translating text
 * using the DeepL API. Tests cover successful translations, retry logic on recoverable errors,
 * asynchronous translations, error handling, and monitoring statistics. Uses Mockito to mock
 * dependencies such as {@link HttpClient} and {@link ObjectMapper}.
 */
class DeepLClientImplTest {

    /**
     * Mocked {@link HttpClient} used to simulate HTTP requests to the DeepL API.
     */
    @Mock
    private HttpClient httpClient;

    /**
     * Mocked {@link HttpResponse} used to simulate HTTP responses from the DeepL API.
     */
    @Mock
    private HttpResponse<String> httpResponse;

    /**
     * Mocked {@link ObjectMapper} used to simulate JSON serialization and deserialization.
     */
    @Mock
    private ObjectMapper objectMapper;

    /**
     * Instance of {@link DeepLClientImpl} under test, configured with a mocked {@link HttpClient}.
     */
    private DeepLClientImpl client;

    /**
     * Configuration for the DeepL client, specifying API URL, authentication key, and other settings.
     */
    private DeepLConfig config;

    /**
     * AutoCloseable resource for managing Mockito mocks, ensuring proper cleanup after each test.
     */
    private AutoCloseable mocks;

    /**
     * Sets up the test environment before each test. Initializes Mockito mocks, configures the
     * {@link DeepLConfig}, creates a {@link DeepLClientImpl} instance, and injects the mocked
     * {@link HttpClient} using reflection. Configures mock behaviors for HTTP responses and JSON
     * serialization/deserialization.
     *
     * @throws Exception if an error occurs during setup, such as reflection access issues.
     */
    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        config = DeepLConfig.builder()
                .apiUrl("https://api-free.deepl.com/v2/translate")
                .authKey("YOUR_API_KEY:fx")
                .enableCache(false)
                .maxRetries(1)
                .build();

        client = new DeepLClientImpl(config, httpClient, objectMapper);

        // Use reflection to inject mock HttpClient
        Field httpClientField = DeepLClientImpl.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(client, httpClient);

        lenient().when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"text\": [\"Hello\"], \"target_lang\": \"ES\"}");
        lenient().when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        lenient().when(httpResponse.statusCode()).thenReturn(200);
        lenient().when(httpResponse.body())
                .thenReturn("{\"translations\": [{\"text\": \"Hola\", \"detected_source_language\": \"EN\"}]}");
        lenient().when(objectMapper.readValue(any(String.class), eq(TranslationResponse.class)))
                .thenReturn(new TranslationResponse(List.of(new Translation("Hola", "EN"))));
    }

    /**
     * Cleans up after each test by closing the {@link DeepLClientImpl} instance and releasing
     * Mockito mocks to prevent resource leaks.
     *
     * @throws Exception if an error occurs during cleanup.
     */
    @AfterEach
    void tearDown() throws Exception {
        client.close();
        mocks.close();
    }

    /**
     * Tests successful translation of text using the {@link DeepLClientImpl#translate} method.
     * Verifies that the translation response contains the expected translated text and detected
     * source language, and that the HTTP client is invoked exactly once.
     *
     * @throws Exception if an error occurs during the translation process.
     */
    @Test
    void shouldTranslateSuccessfully() throws Exception {
        TranslationRequest request = new TranslationRequest(List.of("Hello"), "ES", null);
        TranslationResponse response = client.translate(request);

        assertNotNull(response, "Translation response should not be null");
        assertEquals("Hola", response.getTranslations().get(0).getText(), "Translated text should be 'Hola'");
        System.out.println("Detected language: " + response.getTranslations().get(0).getDetectedSourceLanguage());
        assertEquals("EN", response.getTranslations().get(0).getDetectedSourceLanguage(), "Detected source language should be 'EN'");
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    /**
     * Tests the retry logic of the {@link DeepLClientImpl#translate} method when a recoverable error
     * (HTTP 429 - Rate Limit Exceeded) occurs. Verifies that the client retries the request, succeeds
     * on the second attempt, and returns the expected translation response. Confirms that the HTTP
     * client is invoked twice.
     *
     * @throws Exception if an error occurs during the translation process.
     */
    @Test
    void shouldRetryOnRecoverableError() throws Exception {
        HttpResponse<String> retryResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse, retryResponse);
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn("Rate limit exceeded");
        when(retryResponse.statusCode()).thenReturn(200);
        when(retryResponse.body())
                .thenReturn("{\"translations\": [{\"text\": \"Hola\", \"detected_source_language\": \"EN\"}]}");
        when(objectMapper.readValue(eq("{\"translations\": [{\"text\": \"Hola\", \"detected_source_language\": \"EN\"}]}"), eq(TranslationResponse.class)))
                .thenReturn(new TranslationResponse(List.of(new Translation("Hola", "EN"))));

        TranslationRequest request = new TranslationRequest(List.of("Hello"), "ES", null);
        TranslationResponse response = client.translate(request);

        assertNotNull(response, "Translation response should not be null");
        assertEquals("Hola", response.getTranslations().get(0).getText(), "Translated text should be 'Hola'");
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        System.out.println("Retry test completed with " + Mockito.mockingDetails(httpClient).getInvocations().size() + " invocations");
    }

    /**
     * Tests successful asynchronous translation using the {@link DeepLClientImpl#translateAsync} method.
     * Verifies that the translation response contains the expected translated text and detected
     * source language, and that the HTTP client is invoked exactly once.
     *
     * @throws Exception if an error occurs during the asynchronous translation process.
     */
    @Test
    void shouldTranslateAsyncSuccessfully() throws Exception {
        TranslationRequest request = new TranslationRequest(List.of("Hello"), "ES", null);
        TranslationResponse response = client.translateAsync(request).get();

        assertNotNull(response, "Translation response should not be null");
        assertEquals("Hola", response.getTranslations().get(0).getText(), "Translated text should be 'Hola'");
        assertEquals("EN", response.getTranslations().get(0).getDetectedSourceLanguage(), "Detected source language should be 'EN'");
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    /**
     * Tests the handling of an {@link InterruptedException} during translation. Verifies that the
     * {@link DeepLClientImpl#translate} method throws a {@link DeepLException}, sets the thread's
     * interrupted status, and invokes the HTTP client exactly once.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    void shouldHandleInterruptedException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Interrupted"));

        TranslationRequest request = new TranslationRequest(List.of("Hello"), "ES", null);
        DeepLException exception = assertThrows(
                DeepLException.class,
                () -> client.translate(request),
                "Expected DeepLException for interrupted translation"
        );
        assertEquals("Translation interrupted", exception.getMessage(), "Exception message should indicate interruption");
        assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted");
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    /**
     * Tests the retrieval of monitoring statistics using the {@link DeepLClientImpl#getMonitoringStats} method.
     * Verifies that the initial statistics are empty (no successes, failures, or errors) and that the
     * average latency is zero.
     */
    @Test
    void shouldReturnMonitoringStats() {
        DeepLClientImpl.MonitoringStats stats = client.getMonitoringStats();
        assertNotNull(stats, "Monitoring stats should not be null");
        assertEquals(0, stats.successCount(), "Success count should be 0 initially");
        assertEquals(0, stats.failureCount(), "Failure count should be 0 initially");
        assertEquals(0.0, stats.averageLatencyMillis(), 0.01, "Average latency should be 0.0 initially");
        assertTrue(stats.errorTypeCounts().isEmpty(), "Error type counts should be empty initially");
    }
}