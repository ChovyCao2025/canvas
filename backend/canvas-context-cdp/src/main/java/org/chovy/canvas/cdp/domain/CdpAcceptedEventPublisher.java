package org.chovy.canvas.cdp.domain;

/**
 * 定义 CdpAcceptedEventPublisher 的协作契约。
 */
public interface CdpAcceptedEventPublisher {

    /**
     * 执行 publishAccepted 对应的 CDP 业务操作。
     */
    void publishAccepted(CdpEventLog eventLog);

    /**
     * 执行 noop 对应的 CDP 业务操作。
     */
    static CdpAcceptedEventPublisher noop() {
        return eventLog -> {
        };
    }
}
