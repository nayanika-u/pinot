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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.config.table.ingestion.BatchIngestionConfig;
import org.apache.pinot.spi.config.table.ingestion.IngestionConfig;
import org.apache.pinot.spi.config.table.ingestion.StreamIngestionConfig;
import org.apache.pinot.spi.secretstore.SecretStore;
import org.apache.pinot.spi.secretstore.SecretStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for handling secrets in Pinot.
 */
public class SecretStoreUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretStoreUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SECRET_PREFIX = "SECRET:";

    private SecretStoreUtils() {
        // to avoid init
    }
    /**
     * Creates a standard secret path for a table's credentials.
     *
     * @param tableNameWithType The table name with type suffix
     * @param storePrefix The prefix identifying which backend store to use
     * @return A complete path for the secret service
     */
    public static String createTableCredentialsPath(String tableNameWithType, String storePrefix) {
        return storePrefix + "tables/" + tableNameWithType + "/credentials";
    }

    /**
     * Processes secret information in a table config.
     *
     * @param tableConfig The table configuration to process
     * @param secretStore The secret store to use
     * @param storePrefix The prefix identifying which backend store to use
     * @return true if secrets were processed, false otherwise
     */
    /**
     * Processes secret information in a table config.
     */
    public static boolean processSecretInformation(TableConfig tableConfig, SecretStore secretStore,
                                                   String storePrefix) {
        try {
            // Extract connection credentials from the table config
            Map<String, String> credentials = extractCredentialsFromTableConfig(tableConfig);

            if (!credentials.isEmpty()) {
                // Create a standardized path with the configured prefix
                String secretPath = createTableCredentialsPath(tableConfig.getTableName(), storePrefix);

                // Store in secret service
                String secretKey = secretStore.storeSecret(secretPath, OBJECT_MAPPER.writeValueAsString(credentials));

                // Replace credentials with secret key
                replaceCredentialsWithKey(tableConfig, secretKey);
                return true;
            }
            return false;
        } catch (SecretStoreException | JsonProcessingException e) {
            LOGGER.error("Failed to process secrets for table: {}", tableConfig.getTableName(), e);
            throw new RuntimeException("Failed to process secrets for table configuration", e);
        }
    }

    /**
     * Extracts credential information from a table config.
     */
    /**
     * Extracts credential information from a table config.
     */
    public static Map<String, String> extractCredentialsFromTableConfig(TableConfig tableConfig) {
        Map<String, String> credentials = new HashMap<>();
        Map<String, List<String>> credentialFields = new HashMap<>(); // Track extracted fields by source

        IngestionConfig ingestionConfig = tableConfig.getIngestionConfig();
        if (ingestionConfig != null) {
            // Handle batch ingestion sources
            BatchIngestionConfig batchConfig = ingestionConfig.getBatchIngestionConfig();
            if (batchConfig != null) {
                // Process source configs - this is a list of SegmentGenerationConfig objects
                List<Map<String, String>> sourceConfigs = batchConfig.getBatchConfigMaps();
                if (sourceConfigs != null) {
                    for (Map<String, String> sourceConfig : sourceConfigs) {
                        String sourceType = sourceConfig.getOrDefault("sourceType", "unknown");
                        extractCredentialsForSource(sourceConfig, sourceType, credentials, credentialFields);
                    }
                }
            }

            // Handle stream ingestion sources
            StreamIngestionConfig streamConfig = ingestionConfig.getStreamIngestionConfig();
            if (streamConfig != null) {
                // Get stream configs from StreamIngestionConfig
                List<Map<String, String>> streamConfigs = streamConfig.getStreamConfigMaps();
                if (streamConfigs != null) {
                    for (Map<String, String> streamConfigMap : streamConfigs) {
                        String streamType = streamConfigMap.getOrDefault("streamType", "unknown");
                        extractCredentialsForSource(streamConfigMap, streamType, credentials, credentialFields);
                    }
                }
            }
        }

        // Store the credential field mapping as metadata
        if (!credentialFields.isEmpty()) {
            try {
                credentials.put("__CREDENTIAL_FIELDS__", OBJECT_MAPPER.writeValueAsString(credentialFields));
            } catch (JsonProcessingException e) {
                LOGGER.warn("Failed to serialize credential field map", e);
            }
        }

        return credentials;
    }

    /**
     * Extracts credentials for a specific source type.
     */
    private static void extractCredentialsForSource(
            Map<String, String> sourceConfig,
            String sourceType,
            Map<String, String> credentials,
            Map<String, List<String>> credentialFields) {

        List<String> extractedFields = new ArrayList<>();

        // Handle common credential patterns
        for (Map.Entry<String, String> entry : sourceConfig.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Skip empty values
            if (value == null || value.isEmpty()) {
                continue;
            }

            // Identify credential fields through naming patterns
            if (isCredentialField(key, sourceType)) {
                String credentialKey = sourceType + "." + key;
                credentials.put(credentialKey, value);
                extractedFields.add(key);

                // Mark for replacement in the original config
                sourceConfig.put(key, "TO_BE_REPLACED");
            }
        }

        if (!extractedFields.isEmpty()) {
            credentialFields.put(sourceType, extractedFields);
        }
    }

    /**
     * Determines if a field is a credential field based on field name and source type.
     */
    private static boolean isCredentialField(String fieldName, String sourceType) {
        // Common credential field patterns
        if (fieldName.matches("(?i).*password.*|.*secret.*|.*key.*|.*token.*|.*credential.*|.*auth.*")) {
            return true;
        }

        // Source-specific patterns
        switch (sourceType.toLowerCase()) {
            case "kafka":
            case "confluent-kafka":
                return fieldName.matches("(?i).*sasl\\.jaas\\.config.*|.*ssl\\.keystore\\"
                        + ".password.*|.*ssl\\.key\\.password.*");

            case "kinesis":
                return fieldName.matches("(?i).*accessKey.*|.*secretKey.*|.*sessionToken.*|"
                        + ".*aws\\..*\\.credentials.*");

            case "jdbc":
                return fieldName.matches("(?i).*user.*|.*username.*|.*passwd.*");

            case "s3":
                return fieldName.matches("(?i).*access.*|.*secret.*|.*sessionToken.*|.*roleArn.*");

            case "adls":
            case "azure":
                return fieldName.matches("(?i).*accountKey.*|.*sasToken.*|.*clientId.*"
                        + "|.*clientSecret.*|.*tenantId.*");

            case "gcs":
                return fieldName.matches("(?i).*credential.*|.*privateKey.*|.*privateKeyId.*"
                       + "|.*clientEmail.*");

            case "snowflake":
                return fieldName.matches("(?i).*user.*|.*password.*|.*privateKey.*|.*privateKeyPath.*"
                        + "|.*role.*|.*authenticator.*");

            case "bigquery":
                return fieldName.matches("(?i).*privateKey.*|.*privateKeyId.*|.*clientEmail.*"
                        + "|.*tokenUri.*");
            default:
                LOGGER.error("Unrecognized source type. Fail the credential field validation.");
                return false; // Unrecognized source type. Fail the validation
        }
    }

    /**
     * Replaces credential placeholders with secret references.
     */
    /**
     * Replaces credential placeholders with secret references.
     */
    public static void replaceCredentialsWithKey(TableConfig tableConfig, String secretKey) {
        IngestionConfig ingestionConfig = tableConfig.getIngestionConfig();
        if (ingestionConfig == null) {
            return;
        }

        // Process batch config sources
        BatchIngestionConfig batchConfig = ingestionConfig.getBatchIngestionConfig();
        if (batchConfig != null) {
            // Process segment generation configs
            List<Map<String, String>> sourceConfigs = batchConfig.getBatchConfigMaps();
            if (sourceConfigs != null) {
                for (Map<String, String> sourceConfig : sourceConfigs) {
                    replaceCredentialsInMap(sourceConfig, secretKey);
                }
            }
        }

        // Process stream config
        StreamIngestionConfig streamConfig = ingestionConfig.getStreamIngestionConfig();
        if (streamConfig != null) {
            // Process stream config maps
            List<Map<String, String>> streamConfigs = streamConfig.getStreamConfigMaps();
            if (streamConfigs != null) {
                for (Map<String, String> streamMap : streamConfigs) {
                    replaceCredentialsInMap(streamMap, secretKey);
                }
            }
        }
    }

    /**
     * Helper method to replace TO_BE_REPLACED placeholders with secret references.
     */
    private static void replaceCredentialsInMap(Map<String, String> configMap, String secretKey) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if ("TO_BE_REPLACED".equals(entry.getValue())) {
                entry.setValue(SECRET_PREFIX + secretKey);
            }
        }
    }

    /**
     * Resolves any secret references in the table config.
     */
    /**
     * Resolves any secret references in the table config.
     */
    public static TableConfig resolveSecrets(TableConfig tableConfig, SecretStore secretStore) {
        if (tableConfig == null) {
            return null;
        }

        // Create a copy using JSON serialization/deserialization
        TableConfig resolvedConfig;
        try {
            String tableConfigJson = OBJECT_MAPPER.writeValueAsString(tableConfig);
            resolvedConfig = OBJECT_MAPPER.readValue(tableConfigJson, TableConfig.class);
        } catch (IOException e) {
            LOGGER.error("Failed to create a copy of the table config", e);
            return tableConfig; // Return original if copy fails
        }

        IngestionConfig ingestionConfig = resolvedConfig.getIngestionConfig();
        if (ingestionConfig == null) {
            return resolvedConfig;
        }

        // Process batch config sources
        BatchIngestionConfig batchConfig = ingestionConfig.getBatchIngestionConfig();
        if (batchConfig != null) {
            // Process segment generation configs
            List<Map<String, String>> sourceConfigs = batchConfig.getBatchConfigMaps();
            if (sourceConfigs != null) {
                for (Map<String, String> sourceConfig : sourceConfigs) {
                    resolveSecretsInMap(sourceConfig, secretStore);
                }
            }
        }

        // Process stream config
        StreamIngestionConfig streamConfig = ingestionConfig.getStreamIngestionConfig();
        if (streamConfig != null) {
            // Process stream config maps
            List<Map<String, String>> streamConfigs = streamConfig.getStreamConfigMaps();
            if (streamConfigs != null) {
                for (Map<String, String> streamMap : streamConfigs) {
                    resolveSecretsInMap(streamMap, secretStore);
                }
            }
        }

        return resolvedConfig;
    }

    /**
     * Resolves secret references in a configuration map.
     */
    public static void resolveSecretsInMap(Map<String, String> configMap, SecretStore secretStore) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String value = entry.getValue();
            if (isSecretReference(value)) {
                try {
                    String secretKey = value.substring(SECRET_PREFIX.length());

                    // Get the secret data
                    String secretData = secretStore.getSecret(secretKey);
                    Map<String, Object> secretMap = OBJECT_MAPPER.readValue(secretData,
                            new TypeReference<Map<String, Object>>() { });

                    // Get source type
                    String sourceType = configMap.getOrDefault("sourceType",
                            configMap.getOrDefault("streamType", "unknown"));

                    // Get credential fields mapping
                    if (secretMap.containsKey("__CREDENTIAL_FIELDS__")) {
                        Map<String, List<String>> credentialFields = OBJECT_MAPPER.readValue(
                                (String) secretMap.get("__CREDENTIAL_FIELDS__"),
                                new TypeReference<Map<String, List<String>>>() { });

                        if (credentialFields.containsKey(sourceType)) {
                            List<String> fields = credentialFields.get(sourceType);
                            if (fields.contains(entry.getKey())) {
                                String credentialKey = sourceType + "." + entry.getKey();
                                Object secretValue = secretMap.get(credentialKey);
                                if (secretValue != null) {
                                    entry.setValue(secretValue.toString());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to resolve secret reference: {}", value, e);
                }
            }
        }
    }

    /**
     * Checks if a value is a secret reference.
     */
    public static boolean isSecretReference(String value) {

        return value != null && value.startsWith(SECRET_PREFIX);
    }
}
