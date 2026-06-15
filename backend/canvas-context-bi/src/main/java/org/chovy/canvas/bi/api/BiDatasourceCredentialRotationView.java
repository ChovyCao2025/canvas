package org.chovy.canvas.bi.api;

public record BiDatasourceCredentialRotationView(
        Long id,
        String sourceKey,
        String rotatedBy) {
}
