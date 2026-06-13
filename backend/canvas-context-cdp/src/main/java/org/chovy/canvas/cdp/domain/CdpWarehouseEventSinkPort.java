package org.chovy.canvas.cdp.domain;

public interface CdpWarehouseEventSinkPort {

    void mirrorAccepted(CdpEventLog eventLog);

    static CdpWarehouseEventSinkPort noop() {
        return eventLog -> {
        };
    }
}
