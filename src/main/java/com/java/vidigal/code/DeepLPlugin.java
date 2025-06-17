package com.java.vidigal.code;

import com.java.vidigal.code.builder.TranslationRequestBuilder;
import com.java.vidigal.code.client.DeepLClient;
import com.java.vidigal.code.client.DeepLClientImpl;
import com.java.vidigal.code.exception.DeepLException;
import com.java.vidigal.code.language.LanguageRegistry;
import com.java.vidigal.code.request.Translation;
import com.java.vidigal.code.request.TranslationResponse;
import com.java.vidigal.code.utilities.cache.TranslationCache;
import com.java.vidigal.code.utilities.config.DeepLConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/**
 * A facade for simplified interaction with the DeepL translation API.
 * <p>
 * Provides high-level methods for synchronous and asynchronous translations, supporting single and
 * batch text operations. Integrates with {@link DeepLClient} for API communication,
 * {@link LanguageRegistry} for language validation, and supports caching and monitoring.
 * Ensures proper resource management and validation, using SLF4J for logging.
 * </p>
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * DeepLPlugin plugin = new DeepLPlugin("https://api.deepl.com/v2/translate", "your-auth-key");
 * String translated = plugin.translateText("Bonjour", "EN", "FR");
 * System.out.println(translated); // Outputs: "Hello"
 * plugin.shutdown();
 * }</pre>
 *
 * @author Vidigal
 * @version 1.0
 * @since 1.0
 */
public class DeepLPlugin {

    private static final Logger logger = LoggerFactory.getLogger(DeepLPlugin.class);
    private static final int MAX_BATCH_SIZE = 50;
    private static final String TEXT_NOT_NULL_OR_EMPTY = "Text to translate must not be null or empty";
    private static final String TARGET_LANG_NOT_NULL_OR_EMPTY = "Target language must not be null or empty";
    private static final String TEXT_BATCH_NOT_NULL_OR_EMPTY = "Text batch to translate must not be null or empty";
    private static final UnaryOperator<String> TO_LOWER_CASE = String::toLowerCase;
    private final DeepLClient client;
    private final LanguageRegistry languageRegistry;

    /**
     * Constructs a {@code DeepLPlugin} with the specified API URL and authentication key.
     *
     * @param apiUrl  the DeepL API endpoint URL (e.g., "https://api.deepl.com/v2/translate")
     * @param authKey the authentication key for the DeepL API
     * @throws IllegalArgumentException if {@code apiUrl} or {@code authKey} is null or empty
     */
    public DeepLPlugin(String apiUrl, String authKey) {
        this(DeepLConfig.builder().apiUrl(apiUrl).authKey(authKey).build(), new LanguageRegistry());
    }

    /**
     * Constructs a {@code DeepLPlugin} with a custom configuration and language registry.
     *
     * @param config           the configuration for the DeepL client
     * @param languageRegistry the registry for validating supported languages, or null for default
     * @throws IllegalArgumentException if {@code config} is null
     */
    public DeepLPlugin(DeepLConfig config, LanguageRegistry languageRegistry) {
        if (config == null) {
            logger.error("Configuration cannot be null");
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.client = new DeepLClientImpl(config);
        this.languageRegistry = languageRegistry != null ? languageRegistry : new LanguageRegistry();
    }

    /**
     * Constructs a {@code DeepLPlugin} with a custom configuration.
     *
     * @param config the configuration for the DeepL client
     * @throws IllegalArgumentException if {@code config} is null
     */
    public DeepLPlugin(DeepLConfig config) {
        this(config, new LanguageRegistry());
    }

    /**
     * Translates a single text to the target language with auto-detected source language.
     *
     * @param text       the text to translate
     * @param targetLang the target language code (e.g., "EN", "FR")
     * @return the translated text
     * @throws DeepLException           if translation fails or target language is unsupported
     * @throws IllegalArgumentException if {@code text} or {@code targetLang} is invalid
     */
    public String translate(String text, String targetLang) throws Exception {
        return translateText(text, targetLang, null);
    }

    /**
     * Translates a single text to the target language with auto-detected source language.
     *
     * @param text       the text to translate
     * @param targetLang the target language code (e.g., "EN", "FR")
     * @return the translated text
     * @throws DeepLException           if translation fails or target language is unsupported
     * @throws IllegalArgumentException if {@code text} or {@code targetLang} is invalid
     */
    public String translateText(String text, String targetLang) throws Exception {
        return translateText(text, targetLang, null);
    }

    /**
     * Translates a single text to the target language, optionally specifying the source language.
     *
     * @param text       the text to translate
     * @param targetLang the target language code (e.g., "EN", "FR")
     * @param sourceLang the source language code (e.g., "EN", "FR"), or null for auto-detection
     * @return the translated text
     * @throws DeepLException           if translation fails or languages are unsupported
     * @throws IllegalArgumentException if {@code text} or {@code targetLang} is invalid
     */
    public String translateText(String text, String targetLang, String sourceLang) throws Exception {
        validateInputs(text, targetLang);
        validateLanguage(targetLang, sourceLang);

        TranslationRequestBuilder builder = new TranslationRequestBuilder()
                .addText(text)
                .setTargetLang(targetLang)
                .setSourceLang(sourceLang != null && !sourceLang.isBlank() ? sourceLang : null);

        TranslationResponse response = client.translate(builder.build());
        List<Translation> translations = response.getTranslations();

        if (translations == null || translations.isEmpty()) {
            logger.error("No translations returned for text: {}", text);
            throw new DeepLException("No translations returned");
        }

        String translatedText = translations.getFirst().getText();
        logger.debug("Translated text to {}: {}", targetLang, translatedText);
        return translatedText;
    }

    /**
     * Translates a batch of texts to the target language, optionally specifying the source language.
     *
     * @param texts      the list of texts to translate
     * @param targetLang the target language code (e.g., "EN", "FR")
     * @param sourceLang the source language code (e.g., "EN", "FR"), or null for auto-detection
     * @return a list of translated texts in the order of input texts
     * @throws DeepLException           if translation fails or languages are invalid
     * @throws IllegalArgumentException if {@code texts} or {@code targetLang} is invalid
     */
    public List<String> translateBatch(List<String> texts, String targetLang, String sourceLang) throws Exception {
        validateBatchInputs(texts, targetLang);
        validateLanguage(targetLang, sourceLang);

        TranslationRequestBuilder builder = new TranslationRequestBuilder()
                .setTargetLang(targetLang)
                .setSourceLang(sourceLang != null && !sourceLang.isBlank() ? sourceLang : null);

        for (String text : texts) {
            builder.addText(text);
        }

        TranslationResponse response = client.translate(builder.build());

        List<Translation> translations = response.getTranslations();
        if (translations == null || translations.size() != texts.size()) {
            logger.error("Invalid translation count: expected {}, got {}", texts.size(), translations == null ? 0 : translations.size());
            throw new DeepLException("Invalid translation count");
        }


        List<String> translatedTexts = translations.stream().map(Translation::getText).toList();
        logger.debug("Translated batch of {} texts to {}", texts.size(), targetLang);
        return translatedTexts;
    }

    /**
     * Asynchronously translates a single text to the target language with auto-detected source language.
     *
     * @param text       the text to translate
     * @param targetLang the target language code (e.g., "EN", "FR")
     * @return a {@code CompletableFuture} resolving to the translated text
     * @throws DeepLException           if the target language is invalid
     * @throws IllegalArgumentException if {@code text} or {@code targetLang} is invalid
     */
    public CompletableFuture<String> translateTextAsync(String text, String targetLang) throws Exception {
        return translateTextAsync(text, targetLang, null);
    }

    /**
     * Asynchronously translates a single text to the target language, optionally specifying the source language.
     *
     * @param text       the text to translate
     * @param targetLang the target language code (e.g., "EN", "FR")
     * @param sourceLang the source language code (e.g., "FR"), or null for auto-detection
     * @return a {@code CompletableFuture} resolving to the translated text
     * @throws DeepLException           if languages are invalid
     * @throws IllegalArgumentException if {@code text} or {@code targetLang} is invalid
     */
    public CompletableFuture<String> translateTextAsync(String text, String targetLang, String sourceLang) throws Exception {
        validateInputs(text, targetLang);
        validateLanguage(targetLang, sourceLang);

        TranslationRequestBuilder builder = new TranslationRequestBuilder()
                .addText(text)
                .setTargetLang(targetLang)
                .setSourceLang(sourceLang != null && !sourceLang.isBlank() ? sourceLang : null);

        return client.translateAsync(builder.build())
                .thenApply(response -> {
                    List<Translation> translations = response.getTranslations();
                    if (translations == null || translations.isEmpty()) {
                        logger.error("No translations returned for text: {}", text);
                        try {
                            logger.error("Translated text to {}: {}", targetLang, text);
                            throw new DeepLException("No translations returned");
                        } catch (DeepLException e) {
                            logger.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }
                    String translatedText = translations.getFirst().getText();
                    logger.debug("Translated text to {}: {}", targetLang, translatedText);
                    return translatedText;
                });

    }

    /**
     * Asynchronously translates a batch of texts to the target language, optionally specifying the source language.
     *
     * @param texts      the list of texts to translate
     * @param targetLang the target language code (e.g., "EN", "DE")
     * @param sourceLang the source language code (e.g., "DE"), or null for auto-detection
     * @return a {@code CompletableFuture} resolving to a list of translated texts
     * @throws DeepLException           if languages are invalid
     * @throws IllegalArgumentException if {@code texts} or {@code targetLang} is invalid
     */
    public CompletableFuture<List<String>> translateBatchAsync(List<String> texts, String targetLang, String sourceLang) throws Exception {
        validateBatchInputs(texts, targetLang);
        validateLanguage(targetLang, sourceLang);

        TranslationRequestBuilder builder = new TranslationRequestBuilder()
                .setTargetLang(targetLang)
                .setSourceLang(sourceLang != null && !sourceLang.isBlank() ? sourceLang : null);

        for (String text : texts) {
            builder.addText(text);
        }

        return client.translateAsync(builder.build())
                .thenApply(response -> {
                    List<Translation> translations = response.getTranslations();
                    if (translations == null || translations.size() != texts.size()) {
                        logger.error("Invalid translation count: expected {}, got {}", texts.size(), translations == null ? 0 : translations.size());
                        try {
                            logger.error("Translated batch of {} texts to {}", texts.size(), targetLang);
                            throw new DeepLException("Invalid translation count");
                        } catch (DeepLException e) {
                            try {
                                logger.error(e.getMessage());
                                throw new RuntimeException(e);
                            } catch (RuntimeException ex) {
                                logger.error(ex.getMessage());
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    List<String> translatedTexts = translations.stream().map(Translation::getText).toList();
                    logger.debug("Translated batch of {} texts to {}", texts.size(), targetLang);
                    return translatedTexts;
                })
                .exceptionallyCompose(throwable -> {
                    logger.error("Async batch translation failed", throwable);
                    return CompletableFuture.failedFuture(new DeepLException("Async batch translation failed", throwable.getCause()));
                });
    }

    /**
     * Validates input text and target language.
     *
     * @param text       the text to validate
     * @param targetLang the target language code to validate
     * @throws IllegalArgumentException if inputs are invalid
     */
    private void validateInputs(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            logger.error(TEXT_NOT_NULL_OR_EMPTY);
            throw new IllegalArgumentException(TEXT_NOT_NULL_OR_EMPTY);
        }
        if (targetLang == null || targetLang.isBlank()) {
            logger.error(TARGET_LANG_NOT_NULL_OR_EMPTY);
            throw new IllegalArgumentException(TARGET_LANG_NOT_NULL_OR_EMPTY);
        }
    }

    /**
     * Validates batch input texts and target language.
     *
     * @param texts      the list of texts to validate
     * @param targetLang the target language code to validate
     * @throws IllegalArgumentException if inputs are invalid
     */
    private void validateBatchInputs(List<String> texts, String targetLang) {
        if (texts == null || texts.isEmpty()) {
            logger.error(TEXT_BATCH_NOT_NULL_OR_EMPTY);
            throw new IllegalArgumentException(TEXT_BATCH_NOT_NULL_OR_EMPTY);
        }
        if (targetLang == null || targetLang.isBlank()) {
            logger.error(TARGET_LANG_NOT_NULL_OR_EMPTY);
            throw new IllegalArgumentException(TARGET_LANG_NOT_NULL_OR_EMPTY);
        }
        if (texts.size() > MAX_BATCH_SIZE) {
            logger.error("Batch size {} exceeds maximum: {}", texts.size(), MAX_BATCH_SIZE);
            throw new IllegalArgumentException("Batch size exceeds maximum of " + MAX_BATCH_SIZE);
        }
    }

    /**
     * Validates the target and source languages using the LanguageRegistry.
     *
     * @param targetLang the target language code (e.g., "EN", "FR")
     * @param sourceLang the source language code (e.g., "FR"), or null for auto-detection
     * @throws DeepLException if languages are unsupported
     */
    private void validateLanguage(String targetLang, String sourceLang) throws DeepLException {
        if (!languageRegistry.isSupported(TO_LOWER_CASE.apply(targetLang))) {
            logger.error("Unsupported target language: {}", targetLang);
            throw new DeepLException("Unsupported target language: " + targetLang);
        }
        if (sourceLang != null && !sourceLang.isBlank() && !languageRegistry.isSupported(TO_LOWER_CASE.apply(sourceLang))) {
            logger.error("Unsupported source language: {}", sourceLang);
            throw new DeepLException("Unsupported source language: " + sourceLang);
        }
    }

    /**
     * Retrieves the underlying DeepL client instance.
     *
     * @return the DeepLClient instance
     */
    public DeepLClient getClient() {
        return client;
    }

    /**
     * Retrieves all cache entries from the translation cache, if available.
     *
     * @return a list of cache entries, or an empty list if no cache is available
     */
    public List<TranslationCache.CacheEntry> getAllCache() {
        if (client instanceof DeepLClientImpl impl && impl.getCache() != null) {
            return impl.getCache().getAllCache();
        }
        logger.debug("No cache available, returning empty list");
        return List.of();
    }

    /**
     * Retrieves monitoring statistics for the DeepL client, if available.
     *
     * @return monitoring statistics, or null if unavailable
     */
    public DeepLClientImpl.MonitoringStats getMonitoringStats() {
        if (client instanceof DeepLClientImpl impl) {
            return impl.getMonitoringStats();
        }
        logger.debug("Client is not DeepLClientImpl, returning null stats");
        return null;
    }

    /**
     * Shuts down all resources associated with the DeepL client.
     */
    public void shutdown() {
        if (client instanceof DeepLClientImpl impl) {
            try {
                impl.close();
            } catch (IOException e) {
                logger.error("Failed to close DeepLClientImpl", e);
            }
        }
    }

    /**
     * Clears the translation cache, if available.
     */
    public void clearCache() {
        if (client instanceof DeepLClientImpl impl && impl.getCache() != null) {
            impl.getCache().clear();
        } else {
            logger.debug("No cache available to clear");
        }
    }
}