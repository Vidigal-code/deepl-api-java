package com.java.vidigal.code.exception;

/**
 * Base exception for errors in the DeepL client, such as network issues or API failures.
 */
public class DeepLException extends Exception {

    /**
     * Constructs a new DeepLException with the specified message.
     *
     * @param message The detail message.
     */
    public DeepLException(String message) {
        super(message);
    }

    /**
     * Constructs a new DeepLException with the specified message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause of the exception.
     */
    public DeepLException(String message, Throwable cause) {
        super(message, cause);
    }
}
