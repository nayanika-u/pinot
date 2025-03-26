/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

    private SecretStoreFactory() {
        // to avoid initialization
    }
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
