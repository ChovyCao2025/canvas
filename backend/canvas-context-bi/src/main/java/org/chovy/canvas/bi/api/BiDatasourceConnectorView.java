package org.chovy.canvas.bi.api;

import java.util.List;

public record BiDatasourceConnectorView(
        String connectorType,
        String displayName,
        List<String> credentialFields,
        boolean schemaPreviewSupported,
        boolean apiPreviewSupported) {

    public BiDatasourceConnectorView {
        credentialFields = credentialFields == null ? List.of() : List.copyOf(credentialFields);
    }
}
