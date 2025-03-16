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

/**
 * Interface for managing secrets in Apache Pinot.
 */
public interface SecretStore {
    /**
     * Stores a secret in the secret management system.
     *
     * @param secretName A unique identifier for the secret
     * @param secretValue The actual secret value to be securely stored
     * @return A reference key that can be used later to retrieve the secret
     * @throws SecretStoreException If the secret cannot be stored
     */
    String storeSecret(String secretName, String secretValue) throws SecretStoreException;

    /**
     * Retrieves a secret from the secret management system.
     *
     * @param secretKey The reference key for the secret
     * @return The actual secret value
     * @throws SecretStoreException If the secret cannot be retrieved
     */
    String getSecret(String secretKey) throws SecretStoreException;

    /**
     * Updates an existing secret with a new value.
     *
     * @param secretKey The reference key for the secret
     * @param newSecretValue The new value to store
     * @throws SecretStoreException If the secret cannot be updated
     */
    void updateSecret(String secretKey, String newSecretValue) throws SecretStoreException;

    /**
     * Deletes a secret when it is no longer needed.
     *
     * @param secretKey The reference key for the secret to be deleted
     * @throws SecretStoreException If the secret cannot be deleted
     */
    void deleteSecret(String secretKey) throws SecretStoreException;
}
