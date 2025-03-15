package org.apache.pinot.controller.secretstore;

import org.apache.pinot.controller.ControllerConf;
import org.apache.pinot.spi.secretstore.NoOpSecretStore;
import org.apache.pinot.spi.secretstore.SecretStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating SecretStore implementations.
 */
public class SecretStoreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretStoreFactory.class);

    /**
     * Creates a SecretStore based on controller configuration.
     *
     * @param controllerConf Controller configuration
     * @return SecretStore implementation
     */
    public static SecretStore createSecretStore(ControllerConf controllerConf) {
        if (!controllerConf.isSecretManagementEnabled()) {
            LOGGER.info("Secret management is disabled. Using NoOpSecretStore.");
            return new NoOpSecretStore();
        }

        String secretServiceEndpoint = controllerConf.getSecretServiceEndpoint();
        if (secretServiceEndpoint != null && !secretServiceEndpoint.isEmpty()) {
            LOGGER.info("Creating HTTP-based secret store with endpoint: {}", secretServiceEndpoint);
            return new HttpSecretServiceClient(secretServiceEndpoint);
        }

        LOGGER.warn("No valid secret service endpoint configured, falling back to NoOpSecretStore");
        return new NoOpSecretStore();
    }
}
