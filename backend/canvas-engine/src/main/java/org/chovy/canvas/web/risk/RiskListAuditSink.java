package org.chovy.canvas.web.risk;

/**
 * 风控名单审计写入接口。
 */
public interface RiskListAuditSink {

    /**
     * 记录名单治理事件并返回审计编号。
     */
    String record(Long tenantId, String eventType, String resourceKey, String resourceId, String actor);
}
