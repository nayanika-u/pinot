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

import org.apache.pinot.spi.env.PinotConfiguration;

public interface SecretProvider {

    /**
     * Initialize the provider with configuration
     */
    void init(PinotConfiguration config);

    /**
     * Gets a secret value from the provider
     * @param secretKey the name/path of the secret
     * @return the secret value
     */
    String getSecret(String secretKey);

    /**
     * Checks if this provider can provide the secret with the given path
     * @param secretKey the path or identifier of the secret
     * @return true if this provider supports the secret path, false otherwise
     */
    boolean supportsSecretPath(String secretKey);

    /**
     * Save the secretValue identifiable by secretKey in the underlying secret store.
     *
     * @param secretKey the path or identifier of the secret
     * @param secretValue secret or credentials stored in the secret store
     * */
    void saveSecret(String secretKey, String secretValue);

    /**
     * Deletes the secretValue identifiable by secretKey in the underlying secret store.
     *
     * @param secretKey the path or identifier of the secret
     */
    void deleteSecret(String secretKey);
}

