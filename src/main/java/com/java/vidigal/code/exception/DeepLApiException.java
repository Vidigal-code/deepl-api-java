package com.java.vidigal.code.exception;

/**
 * Exception for failures in DeepL API requests, including HTTP status code.
 */
public class DeepLApiException extends DeepLException {
    private final int statusCode;

    /**
     * Constructs a new DeepLApiException with the specified message and status code.
     *
     * @param message    The detail message.
     * @param statusCode The HTTP status code of the failed request.
     */
    public DeepLApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return The HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
