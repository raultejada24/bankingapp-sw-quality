package es.codeurjc.service.exceptions;

/**
 * Exception thrown for invalid banking operations.
 * Issue 13: Use custom exceptions instead of generic IllegalArgumentException
 */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
