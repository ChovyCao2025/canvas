package org.chovy.canvas.cdp.domain;

public interface CdpPrivacyTombstonePort {

    void enforceNotBlocked(Long tenantId, String subjectType, String subjectValue, String source);

    static CdpPrivacyTombstonePort allowAll() {
        return (tenantId, subjectType, subjectValue, source) -> {
        };
    }
}
