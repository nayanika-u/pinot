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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.pinot.spi.secretstore.SecretStore;
import org.apache.pinot.spi.secretstore.SecretStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SecretStore interface that communicates with the Secret Service via HTTP.
 */
/**
 * Implementation of SecretStore interface that communicates with the Secret Service via HTTP.
 */
public class HttpSecretServiceClient implements SecretStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSecretServiceClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String _baseUrl;
    private final CloseableHttpClient _httpClient;
    // check how DM generates auth token
    private final String _authToken;
    private final String _storePrefix;

    public HttpSecretServiceClient(String baseUrl) {
        this(baseUrl, "eyJhbGciOiJSUzI1NiIsImtpZCI6ImFlZjM2YWRlNzc0MD" +
                        "A5YTg4MGMyZDRiNmFlNDY2MzMwZjI3OWNlZjcifQ.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnV0NnUzaS5jcC5zN2Uuc3RhcnRyZWUtZGV2LmNsb3VkIiwic3ViIjoiQ2lObmIyOW5iR1V0YjJGMWRHZ3lmREV3TkRVek5UZ3hNRE14TXpjNU5qTTRNRFV3TkJJSWMzUmhjblJ5WldVIiwiYXVkIjoiZGF0YS1tYW5hZ2VyLXVpIiwiZXhwIjoxNzQzMjMyMjEwLCJpYXQiOjE3NDMxNDU4MTAsIm5vbmNlIjoicmFuZG9tX3N0cmluZyIsImF0X2hhc2giOiI0Z2xCaGY0WGYyZVNSbFpLbnE1OTFnIiwiZW1haWwiOiJuYXlhbmlrYUBzdGFydHJlZS5haSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJncm91cHMiOlsic3RhcnRyZWUtb3BzLXN5c3RlbS1hZG1pbi1zcmUiXSwibmFtZSI6Ik5heWFuaWthIFVwYWRoeWF5In0.eFqfUWZZ2R0k1z4vlnpaluGqwZ5P8Idwy1WGjW32IHtFsDdhGTdjh4i8hIna7UF-PZWVTMeCCku_FyuLv7BgltyDI13e0OTXbbVYspsVVYv9IxlVZtwP4vDzn1oyiFiMojEiM0yD0ex-EA-fE6HpmRYFCEDbImxcfFdrGYW-y5dPlOQ2bF_LkZp2G-OCzREts6BkKFk0ZqtW1ILr6KEh_-SjyHheVr0pu_nPIYq0VCgG7IsQJZFSdilkSDDkkSYsLcJffY35C36PFEzmfzpkSCgQe6I_UTkPX5bGW6WmB76gSrTZ1dztX78H9yUGBh1Pyv4XRQ3gDuErZEPPtGJOvw", "startree/");
    }

    public HttpSecretServiceClient(String baseUrl, String authToken, String storePrefix) {
        _baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        _httpClient = HttpClients.createDefault();
        _authToken = authToken;
        _storePrefix = storePrefix;
    }

    @Override
    public String storeSecret(String secretKey, String secretValue) throws SecretStoreException {
        try {
            // Format path to include store prefix
            String path = _storePrefix + secretKey;

            // Prepare request body
            Map<String, Object> data = new HashMap<>();
            data.put("value", secretValue);

            Map<String, Object> secretObj = new HashMap<>();
            secretObj.put("data", data);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("path", path);
            requestBody.put("secret", secretObj);

            // POST to the create/update endpoint
            String url = _baseUrl + "secret/v1/secret";
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");

            if (_authToken != null) {
                request.setHeader("Authorization", "Bearer " + _authToken);
            }

            request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(requestBody),
                    ContentType.APPLICATION_JSON));

            HttpResponse response = _httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : null;

            if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_OK) {
                LOGGER.error("Failed to store secret. Status: {}, Response: {}", statusCode, responseBody);
                throw new SecretStoreException("Failed to store secret. Status: "
                        + statusCode + ", Response: " + responseBody);
            }

            return secretKey;
        } catch (IOException e) {
            throw new SecretStoreException("Error while storing secret: " + secretKey, e);
        }
    }

    @Override
    public String getSecret(String secretKey) throws SecretStoreException {
        try {
            // Format path to include store prefix
            String path = _storePrefix + secretKey;

            // GET from the get endpoint with path parameter
            String url = _baseUrl + "secret/v1/secret/" + path;
            HttpGet request = new HttpGet(url);
            request.setHeader("Content-Type", "application/json");

            if (_authToken != null) {
                request.setHeader("Authorization", "Bearer " + _authToken);
            }

            HttpResponse response = _httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : null;

            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.error("Failed to retrieve secret. Status: {}, Response: {}", statusCode, responseBody);
                throw new SecretStoreException("Failed to retrieve secret. Status: "
                        + statusCode + ", Response: " + responseBody);
            }

            // Parse the response based on your proto definition
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            if (!root.has("secret") || !root.get("secret").has("data") || !root.get("secret").get("data").has("value")) {
                throw new SecretStoreException("Secret response did not contain expected fields");
            }

            return root.get("secret").get("data").get("value").asText();
        } catch (IOException e) {
            throw new SecretStoreException("Error while retrieving secret: " + secretKey, e);
        }
    }

    @Override
    public void updateSecret(String secretKey, String newSecretValue) throws SecretStoreException {
        // Your API uses the same endpoint for create and update
        storeSecret(secretKey, newSecretValue);
    }

    @Override
    public void deleteSecret(String secretKey) throws SecretStoreException {
        try {
            // Format path to include store prefix
            String path = _storePrefix + secretKey;

            // DELETE from the delete endpoint with path parameter
            String url = _baseUrl + "secret/v1/secret/" + path;
            HttpDelete request = new HttpDelete(url);
            request.setHeader("Content-Type", "application/json");

            if (_authToken != null) {
                request.setHeader("Authorization", "Bearer " + _authToken);
            }

            HttpResponse response = _httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NO_CONTENT) {
                String responseBody = EntityUtils.toString(response.getEntity());
                LOGGER.error("Failed to delete secret. Status: {}, Response: {}", statusCode, responseBody);
                throw new SecretStoreException("Failed to delete secret. Status: "
                        + statusCode + ", Response: " + responseBody);
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
