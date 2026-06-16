package org.chovy.canvas.cdp.domain;

/**
 * 定义 CdpWarehouseEventSinkPort 的协作契约。
 */
public interface CdpWarehouseEventSinkPort {

    /**
     * 执行 mirrorAccepted 对应的 CDP 业务操作。
     */
    void mirrorAccepted(CdpEventLog eventLog);

    /**
     * 执行 noop 对应的 CDP 业务操作。
     */
    static CdpWarehouseEventSinkPort noop() {
        return eventLog -> {
        };
    }
}
