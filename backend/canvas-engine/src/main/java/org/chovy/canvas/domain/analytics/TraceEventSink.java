package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AnalyticsEventDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;

import java.util.List;

public interface TraceEventSink {

    void writeTraces(List<CanvasExecutionTraceDO> traces);

    void writeEvents(List<AnalyticsEventDO> events);

    SinkMetrics metrics();

    record SinkMetrics(long writtenCount, long failedCount, long droppedCount, long backlog) {
    }
}
