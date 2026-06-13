package org.chovy.canvas.cdp.domain;

public interface CdpEventRepository {

    boolean existsByMessageId(Long tenantId, String messageId);

    boolean existsByIdempotencyKey(Long tenantId, String idempotencyKey);

    boolean save(CdpEventLog eventLog);
}
