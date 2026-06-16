package org.chovy.canvas.bi.domain;
/**
 * BiDatasourceHealthPort 接口契约。
 */
public interface BiDatasourceHealthPort {
    /**
     * 执行 find Latest Health 相关处理。
     */

    BiDatasourceHealth findLatestHealth(Long tenantId, BiResourceKey sourceKey);
}
