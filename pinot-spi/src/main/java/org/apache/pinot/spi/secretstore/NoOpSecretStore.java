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
