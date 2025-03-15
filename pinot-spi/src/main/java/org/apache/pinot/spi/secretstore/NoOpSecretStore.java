package org.apache.pinot.spi.secretstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A no-op implementation of SecretStore that doesn't actually store or retrieve secrets.
 * This is the default implementation for backward compatibility when no secret management is needed.
 */
public class NoOpSecretStore implements SecretStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpSecretStore.class);

    @Override
    public String storeSecret(String secretName, String secretValue) throws SecretStoreException {
        LOGGER.info("Using NoOpSecretStore: not actually storing secret for: {}", secretName);
        // Just return the original value - no actual secret management happens
        return secretName;
    }

    @Override
    public String getSecret(String secretKey) throws SecretStoreException {
        LOGGER.info("Using NoOpSecretStore: not actually retrieving secret for: {}", secretKey);
        // Return an empty JSON string
        return "{}";
    }

    @Override
    public void updateSecret(String secretKey, String newSecretValue) throws SecretStoreException {
        LOGGER.info("Using NoOpSecretStore: not actually updating secret for: {}", secretKey);
        // No-op
    }

    @Override
    public void deleteSecret(String secretKey) throws SecretStoreException {
        LOGGER.info("Using NoOpSecretStore: not actually deleting secret for: {}", secretKey);
        // No-op
    }
}