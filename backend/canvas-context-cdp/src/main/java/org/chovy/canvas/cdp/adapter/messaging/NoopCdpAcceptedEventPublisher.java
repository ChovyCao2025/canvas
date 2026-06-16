package org.chovy.canvas.cdp.adapter.messaging;

import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 表示 NoopCdpAcceptedEventPublisher 的业务数据或处理组件。
 */
@Component
@ConditionalOnMissingBean(CdpAcceptedEventPublisher.class)
public class NoopCdpAcceptedEventPublisher implements CdpAcceptedEventPublisher {

    /**
     * 执行 publishAccepted 对应的 CDP 业务操作。
     */
    @Override
    public void publishAccepted(CdpEventLog eventLog) {
        // Integration publishers are supplied by boot/cutover wiring.
    }
}
