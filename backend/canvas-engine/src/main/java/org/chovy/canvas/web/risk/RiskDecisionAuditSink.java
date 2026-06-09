package org.chovy.canvas.web.risk;

/**
 * 风控决策审计写入接口。
 */
public interface RiskDecisionAuditSink {

    /**
     * 记录请求体租户编号被忽略的审计事件。
     */
    void tenantOverrideIgnored(Long authenticatedTenantId, Long bodyTenantId, String actor);
}
