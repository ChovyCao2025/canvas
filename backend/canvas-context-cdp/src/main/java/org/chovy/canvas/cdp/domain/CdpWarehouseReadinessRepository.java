package org.chovy.canvas.cdp.domain;

/**
 * 定义 CdpWarehouseReadiness 的持久化访问契约。
 */
public interface CdpWarehouseReadinessRepository {

    /**
     * 执行 evidence 对应的 CDP 业务操作。
     */
    CdpWarehouseReadinessEvidence evidence(Long tenantId);
}
