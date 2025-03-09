package org.apache.pinot.spi.secretstore.impl;

package org.apache.pinot.spi.secretstore.impl;

import org.apache.pinot.spi.secretstore.SecretStore;
import org.apache.pinot.spi.secretstore.SecretStoreException;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of SecretStore interface that communicates with the Secret Service
 * via gRPC.
 */
public class SecretServiceClient implements SecretStore {
    private final SecretManagerGrpc.SecretManagerBlockingStub client;

    /**
     * Constructs a new SecretServiceClient with the provided gRPC client stub.
     *
     * @param client The gRPC client stub to use for communication with the Secret Service
     */
    public SecretServiceClient(SecretManagerGrpc.SecretManagerBlockingStub client) {
        this.client = client;
    }

    @Override
    public String storeSecret(String secretName, String secretValue, Map<String, String> metadata)
            throws SecretStoreException {
        try {
            List<SecretMetadata> metadataList = convertMapToMetadata(metadata);
            List<SecretData> secretData = List.of(SecretData.newBuilder()
                    .setKey("value")
                    .setVal(secretValue)
                    .build());

            Secret secret = Secret.newBuilder()
                    .setPath(secretName)
                    .addAllData(secretData)
                    .addAllMetadata(metadataList)
                    .build();

            CreateUpdateSecretRequest request = CreateUpdateSecretRequest.newBuilder()
                    .setSecret(secret)
                    .setAuth(buildAuthMetadata())
                    .build();

            CreateUpdateSecretResponse response = client.createUpdateSecret(request);
            return secretName; // The path serves as the key
        } catch (StatusRuntimeException e) {
            throw new SecretStoreException("Failed to store secret: " + secretName, e);
        } catch (Exception e) {
            throw new SecretStoreException("Unexpected error while storing secret", e);
        }
    }

    @Override
    public String getSecret(String secretKey) throws SecretStoreException {
        try {
            GetSecretRequest request = GetSecretRequest.newBuilder()
                    .setPath(secretKey)
                    .setAuth(buildAuthMetadata())
                    .build();

            GetSecretResponse response = client.getSecret(request);

            // Extract and return the actual secret value
            return response.getSecret().getDataList().stream()
                    .filter(data -> "value".equals(data.getKey()))
                    .findFirst()
                    .map(SecretData::getVal)
                    .orElseThrow(() -> new SecretStoreException("Secret value not found in response"));
        } catch (StatusRuntimeException e) {
            throw new SecretStoreException("Failed to retrieve secret: " + secretKey, e);
        } catch (Exception e) {
            throw new SecretStoreException("Unexpected error while retrieving secret", e);
        }
    }

    @Override
    public void updateSecret(String secretKey, String newSecretValue, Map<String, String> metadata)
            throws SecretStoreException {
        try {
            List<SecretMetadata> metadataList = convertMapToMetadata(metadata);
            List<SecretData> secretData = List.of(SecretData.newBuilder()
                    .setKey("value")
                    .setVal(newSecretValue)
                    .build());

            Secret secret = Secret.newBuilder()
                    .setPath(secretKey)
                    .addAllData(secretData)
                    .addAllMetadata(metadataList)
                    .build();

            CreateUpdateSecretRequest request = CreateUpdateSecretRequest.newBuilder()
                    .setSecret(secret)
                    .setAuth(buildAuthMetadata())
                    .build();

            client.createUpdateSecret(request);
        } catch (StatusRuntimeException e) {
            throw new SecretStoreException("Failed to update secret: " + secretKey, e);
        } catch (Exception e) {
            throw new SecretStoreException("Unexpected error while updating secret", e);
        }
    }

    @Override
    public void deleteSecret(String secretKey) throws SecretStoreException {
        try {
            DeleteSecretRequest request = DeleteSecretRequest.newBuilder()
                    .setPath(secretKey)
                    .setAuth(buildAuthMetadata())
                    .build();

            client.deleteSecret(request);
        } catch (StatusRuntimeException e) {
            throw new SecretStoreException("Failed to delete secret: " + secretKey, e);
        } catch (Exception e) {
            throw new SecretStoreException("Unexpected error while deleting secret", e);
        }
    }

    /**
     * Builds the authorization metadata for the request.
     * This may involve fetching credentials, tokens, etc.
     *
     * @return Authorization object for the request
     */
    private Authorization buildAuthMetadata() {
        // This would be implemented according to your authentication requirements
        return Authorization.newBuilder()
                // Add required auth fields
                .build();
    }

    /**
     * Converts a Java Map to a list of SecretMetadata objects.
     *
     * @param metadata Map of metadata key-value pairs
     * @return List of SecretMetadata objects
     */
    private List<SecretMetadata> convertMapToMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return new ArrayList<>();
        }

        return metadata.entrySet().stream()
                .map(entry -> SecretMetadata.newBuilder()
                        .setKey(entry.getKey())
                        .setVal(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
