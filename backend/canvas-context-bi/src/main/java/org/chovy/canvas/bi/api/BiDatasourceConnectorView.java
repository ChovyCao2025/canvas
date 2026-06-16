package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiDatasourceConnectorView 视图。
 */
public record BiDatasourceConnectorView(
        /**
         * connectorType 字段值。
         */
        String connectorType,
        /**
         * displayName 字段值。
         */
        String displayName,
        /**
         * credentialFields 对应的数据集合。
         */
        List<String> credentialFields,
        /**
         * schemaPreviewSupported 字段值。
         */
        boolean schemaPreviewSupported,
        boolean apiPreviewSupported) {

    public BiDatasourceConnectorView {
        credentialFields = credentialFields == null ? List.of() : List.copyOf(credentialFields);
    }
}
