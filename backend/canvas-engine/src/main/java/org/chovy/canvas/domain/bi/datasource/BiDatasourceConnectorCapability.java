package org.chovy.canvas.domain.bi.datasource;

import java.util.List;

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
