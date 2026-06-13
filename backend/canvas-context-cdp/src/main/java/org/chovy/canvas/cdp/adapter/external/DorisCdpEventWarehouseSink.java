package org.chovy.canvas.cdp.adapter.external;

import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(DorisCdpEventWarehouseSink.DorisCdpEventWriter.class)
public class DorisCdpEventWarehouseSink implements CdpWarehouseEventSinkPort {

    private final DorisCdpEventWriter writer;

    public DorisCdpEventWarehouseSink(DorisCdpEventWriter writer) {
        this.writer = writer;
    }

    @Override
    public void mirrorAccepted(CdpEventLog eventLog) {
        writer.writeAccepted(eventLog);
    }

    public interface DorisCdpEventWriter {
        void writeAccepted(CdpEventLog eventLog);
    }
}
