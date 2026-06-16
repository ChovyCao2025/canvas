package org.chovy.canvas.bi.api;
/**
 * BiDatasourceCredentialRotationView 视图。
 */
public record BiDatasourceCredentialRotationView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        String rotatedBy) {
}
