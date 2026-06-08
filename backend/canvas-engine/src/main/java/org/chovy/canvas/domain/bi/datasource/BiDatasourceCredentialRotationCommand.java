package org.chovy.canvas.domain.bi.datasource;

/**
 * BiDatasourceCredentialRotationCommand 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param password password 字段。
 */
public record BiDatasourceCredentialRotationCommand(
        String password
) {
}
