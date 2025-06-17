package com.java.vidigal.code.client;

import com.java.vidigal.code.exception.DeepLException;
import com.java.vidigal.code.request.TranslationRequest;
import com.java.vidigal.code.request.TranslationResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for interacting with the DeepL translation API.
 * <p>
 * Provides methods for performing synchronous and asynchronous translation requests
 * using the DeepL API.
 * </p>
 *
 * @author Vidigal
 */
public interface DeepLClient {

    /**
     * Translates text synchronously using the DeepL API.
     *
     * @param request the translation request containing text and language details
     * @return the translation response from the API
     * @throws DeepLException if the translation fails due to API or network issues
     */
    TranslationResponse translate(TranslationRequest request) throws DeepLException;

    /**
     * Translates text asynchronously using the DeepL API with virtual threads.
     *
     * @param request the translation request containing text and language details
     * @return a {@link CompletableFuture} containing the translation response
     */
    CompletableFuture<TranslationResponse> translateAsync(TranslationRequest request);
}
