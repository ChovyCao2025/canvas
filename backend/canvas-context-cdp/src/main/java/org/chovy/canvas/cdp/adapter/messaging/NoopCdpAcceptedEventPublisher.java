package org.chovy.canvas.cdp.adapter.messaging;

import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(CdpAcceptedEventPublisher.class)
public class NoopCdpAcceptedEventPublisher implements CdpAcceptedEventPublisher {

    @Override
    public void publishAccepted(CdpEventLog eventLog) {
        // Integration publishers are supplied by boot/cutover wiring.
    }
}
