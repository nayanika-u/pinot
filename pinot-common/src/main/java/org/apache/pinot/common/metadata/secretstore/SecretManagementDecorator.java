package org.apache.pinot.common.metadata.secretstore;

import org.apache.helix.store.zk.ZkHelixPropertyStore;
import org.apache.helix.zookeeper.datamodel.ZNRecord;
import org.apache.pinot.common.metadata.ZKMetadataDecorator;
import org.apache.pinot.common.secretstore.SecretStoreUtils;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.secretstore.NoOpSecretStore;
import org.apache.pinot.spi.secretstore.SecretStore;

public class SecretManagementDecorator implements ZKMetadataDecorator {
    private final SecretStore _secretStore;
    private final String _secretStorePrefix;

    public SecretManagementDecorator(SecretStore secretStore, String secretStorePrefix) {
        _secretStore = secretStore;
        _secretStorePrefix = secretStorePrefix;
    }

    @Override
    public void beforeSetTableConfig(TableConfig tableConfig, ZkHelixPropertyStore<ZNRecord> propertyStore) {
        if (!(_secretStore instanceof NoOpSecretStore)) {
            SecretStoreUtils.processSecretInformation(tableConfig, _secretStore, _secretStorePrefix);
        }
    }

    @Override
    public TableConfig afterGetTableConfig(TableConfig tableConfig, ZkHelixPropertyStore<ZNRecord> propertyStore) {
        if (tableConfig != null && !(_secretStore instanceof NoOpSecretStore)) {
            return SecretStoreUtils.resolveSecrets(tableConfig, _secretStore);
        }
        return tableConfig;
    }
}
