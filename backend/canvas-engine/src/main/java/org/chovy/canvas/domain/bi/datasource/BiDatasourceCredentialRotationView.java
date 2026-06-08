package org.chovy.canvas.domain.bi.datasource;

/**
 * BiDatasourceCredentialRotationView 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param sourceKey sourceKey 字段。
 * @param rotatedBy rotatedBy 字段。
 */
public record BiDatasourceCredentialRotationView(
        Long id,
        String sourceKey,
        String rotatedBy
) {
}
