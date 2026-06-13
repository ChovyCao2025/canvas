package org.chovy.canvas.cdp.domain;

public interface CdpAcceptedEventPublisher {

    void publishAccepted(CdpEventLog eventLog);

    static CdpAcceptedEventPublisher noop() {
        return eventLog -> {
        };
    }
}
