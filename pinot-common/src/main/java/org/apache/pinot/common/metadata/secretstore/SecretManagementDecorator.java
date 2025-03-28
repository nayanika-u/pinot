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

    public SecretManagementDecorator(SecretStore secretStore) {
        _secretStore = secretStore;
    }

    @Override
    public void beforeSetTableConfig(TableConfig tableConfig, ZkHelixPropertyStore<ZNRecord> propertyStore) {
        if (!(_secretStore instanceof NoOpSecretStore)) {
            SecretStoreUtils.processSecretInformation(tableConfig, _secretStore);
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
