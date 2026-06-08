package org.chovy.canvas.domain.bi.datasource;

import java.util.List;

/**
 * BiDatasourceConnectorCapability 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param connectorType connectorType 字段。
 * @param label label 字段。
 * @param sourceCategory sourceCategory 字段。
 * @param supportedModes supportedModes 字段。
 * @param supportStatus supportStatus 字段。
 * @param capacityCategory capacityCategory 字段。
 * @param capacityNote capacityNote 字段。
 * @param supportsConnectionTest supportsConnectionTest 字段。
 * @param supportsSchemaSync supportsSchemaSync 字段。
 * @param supportsSqlDataset supportsSqlDataset 字段。
 * @param supportsTableDataset supportsTableDataset 字段。
 * @param supportsCredentials supportsCredentials 字段。
 * @param driverClassNames driverClassNames 字段。
 * @param note note 字段。
 */
public record BiDatasourceConnectorCapability(
        String connectorType,
        String label,
        String sourceCategory,
        List<String> supportedModes,
        String supportStatus,
        String capacityCategory,
        String capacityNote,
        boolean supportsConnectionTest,
        boolean supportsSchemaSync,
        boolean supportsSqlDataset,
        boolean supportsTableDataset,
        boolean supportsCredentials,
        List<String> driverClassNames,
        String note
) {

    public BiDatasourceConnectorCapability {
        supportedModes = supportedModes == null ? List.of() : List.copyOf(supportedModes);
        driverClassNames = driverClassNames == null ? List.of() : List.copyOf(driverClassNames);
    }
}
