package org.chovy.canvas.cdp.domain;

/**
 * 定义 CdpEvent 的持久化访问契约。
 */
public interface CdpEventRepository {

    /**
     * 执行 existsByMessageId 对应的 CDP 业务操作。
     */
    boolean existsByMessageId(Long tenantId, String messageId);

    /**
     * 执行 existsByIdempotencyKey 对应的 CDP 业务操作。
     */
    boolean existsByIdempotencyKey(Long tenantId, String idempotencyKey);

    /**
     * 保存save。
     */
    boolean save(CdpEventLog eventLog);
}
