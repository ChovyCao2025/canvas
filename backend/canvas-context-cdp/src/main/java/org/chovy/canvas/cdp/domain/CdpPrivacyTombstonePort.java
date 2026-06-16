package org.chovy.canvas.cdp.domain;

/**
 * 定义 CdpPrivacyTombstonePort 的协作契约。
 */
public interface CdpPrivacyTombstonePort {

    /**
     * 执行 enforceNotBlocked 对应的 CDP 业务操作。
     */
    void enforceNotBlocked(Long tenantId, String subjectType, String subjectValue, String source);

    /**
     * 执行 allowAll 对应的 CDP 业务操作。
     */
    static CdpPrivacyTombstonePort allowAll() {
        return (tenantId, subjectType, subjectValue, source) -> {
        };
    }
}
