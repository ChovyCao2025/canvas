package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpEventLogDO;

public interface CdpWarehouseEventSink {

    void writeAccepted(CdpEventLogDO event);
}
