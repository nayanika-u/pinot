package org.apache.pinot.spi.secretstore;

/**
 * Exception thrown when operations on the {@link SecretStore} fail.
 * This exception encapsulates errors that may occur during secret storage,
 * retrieval, updating, or deletion operations.
 */
public class SecretStoreException extends RuntimeException {

    /**
     * Creates a new SecretStoreException with the specified message.
     *
     * @param message the detail message
     */
    public SecretStoreException(String message) {
        super(message);
    }

    /**
     * Creates a new SecretStoreException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public SecretStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new SecretStoreException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public SecretStoreException(Throwable cause) {
        super(cause);
    }
}
