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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.pinot.spi.secretstore.SecretStore;
import org.apache.pinot.spi.secretstore.SecretStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of SecretStore interface that communicates with the Secret Service via HTTP.
 */
public class HttpSecretServiceClient implements SecretStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSecretServiceClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String _baseUrl;
    private final CloseableHttpClient _httpClient;

    public HttpSecretServiceClient(String baseUrl) {
        _baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        _httpClient = HttpClients.createDefault();
    }

    @Override
    public String storeSecret(String secretName, String secretValue) throws SecretStoreException {
        try {
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("value", secretValue);

            String url = _baseUrl + "secrets/" + secretName;
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

            HttpResponse response = _httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new SecretStoreException("Failed to store secret. Status: " + statusCode + ", Response: " + responseBody);
            }

            return secretName;
        } catch (IOException e) {
            throw new SecretStoreException("Error while storing secret: " + secretName, e);
        }
    }

    @Override
    public String getSecret(String secretKey) throws SecretStoreException {
        try {
            String url = _baseUrl + "secrets/" + secretKey;
            HttpGet request = new HttpGet(url);

            HttpResponse response = _httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : null;

            if (statusCode != HttpStatus.SC_OK) {
                throw new SecretStoreException("Failed to retrieve secret. Status: " + statusCode + ", Response: " + responseBody);
            }

            // Extract the value field from the response
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            if (!root.has("value")) {
                throw new SecretStoreException("Secret response did not contain value field");
            }

            return root.get("value").asText();
        } catch (IOException e) {
            throw new SecretStoreException("Error while retrieving secret: " + secretKey, e);
        }
    }

    @Override
    public void updateSecret(String secretKey, String newSecretValue) throws SecretStoreException {
        try {
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("value", newSecretValue);

            String url = _baseUrl + "secrets/" + secretKey;
            HttpPut request = new HttpPut(url);
            request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

            HttpResponse response = _httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new SecretStoreException("Failed to update secret. Status: " + statusCode + ", Response: " + responseBody);
            }
        } catch (IOException e) {
            throw new SecretStoreException("Error while updating secret: " + secretKey, e);
        }
    }

    @Override
    public void deleteSecret(String secretKey) throws SecretStoreException {
        try {
            String url = _baseUrl + "secrets/" + secretKey;
            HttpDelete request = new HttpDelete(url);

            HttpResponse response = _httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NO_CONTENT) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new SecretStoreException("Failed to delete secret. Status: " + statusCode + ", Response: " + responseBody);
            }
        } catch (IOException e) {
            throw new SecretStoreException("Error while deleting secret: " + secretKey, e);
        }
    }

    // Close the HTTP client when the instance is no longer needed
    public void close() {
        try {
            _httpClient.close();
        } catch (IOException e) {
            LOGGER.warn("Error closing HTTP client", e);
        }
    }
}