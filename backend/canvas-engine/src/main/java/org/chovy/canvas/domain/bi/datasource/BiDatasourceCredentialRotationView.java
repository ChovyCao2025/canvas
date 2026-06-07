package org.chovy.canvas.domain.bi.datasource;

public record BiDatasourceCredentialRotationView(
        Long id,
        String sourceKey,
        String rotatedBy
) {
}
