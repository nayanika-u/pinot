package org.apache.pinot.common.metadata;

import org.apache.helix.store.zk.ZkHelixPropertyStore;
import org.apache.helix.zookeeper.datamodel.ZNRecord;
import org.apache.pinot.spi.config.table.TableConfig;

public interface ZKMetadataDecorator {
    void beforeSetTableConfig(TableConfig tableConfig, ZkHelixPropertyStore<ZNRecord> propertyStore);
    TableConfig afterGetTableConfig(TableConfig tableConfig, ZkHelixPropertyStore<ZNRecord> propertyStore);
}