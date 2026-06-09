package org.chovy.canvas.domain.analytics;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.AnalyticsEventDO;
import org.chovy.canvas.dal.dataobject.AnalyticsEventTraceDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.chovy.canvas.dal.mapper.AnalyticsEventTraceMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class MySqlTraceEventSink implements TraceEventSink {

    private final CanvasExecutionTraceMapper traceMapper;
    private final AnalyticsEventTraceMapper analyticsTraceMapper;
    private final AnalyticsEventMapper analyticsEventMapper;
    private final AtomicLong writtenCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();

    public MySqlTraceEventSink(CanvasExecutionTraceMapper traceMapper,
                               AnalyticsEventTraceMapper analyticsTraceMapper,
                               AnalyticsEventMapper analyticsEventMapper) {
        this.traceMapper = traceMapper;
        this.analyticsTraceMapper = analyticsTraceMapper;
        this.analyticsEventMapper = analyticsEventMapper;
    }

    @Override
    public void writeTraces(List<CanvasExecutionTraceDO> traces) {
        if (traces == null || traces.isEmpty()) {
            return;
        }
        try {
            traceMapper.insertBatch(traces);
            if (analyticsTraceMapper != null) {
                for (CanvasExecutionTraceDO trace : traces) {
                    analyticsTraceMapper.insert(toAnalyticsTrace(trace));
                }
            }
            writtenCount.addAndGet(traces.size());
        } catch (Exception e) {
            failedCount.addAndGet(traces.size());
            log.error("[TRACE_SINK] trace batch write failed: {}", e.getMessage());
        }
    }

    @Override
    public void writeEvents(List<AnalyticsEventDO> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        if (analyticsEventMapper == null) {
            failedCount.addAndGet(events.size());
            log.error("[TRACE_SINK] analytics event mapper is not configured");
            return;
        }
        try {
            for (AnalyticsEventDO event : events) {
                analyticsEventMapper.insert(defaultEventFields(event));
            }
            writtenCount.addAndGet(events.size());
        } catch (Exception e) {
            failedCount.addAndGet(events.size());
            log.error("[TRACE_SINK] analytics event write failed: {}", e.getMessage());
        }
    }

    @Override
    public SinkMetrics metrics() {
        return new SinkMetrics(writtenCount.get(), failedCount.get(), 0, 0);
    }

    private AnalyticsEventTraceDO toAnalyticsTrace(CanvasExecutionTraceDO trace) {
        LocalDateTime eventTime = firstNonNull(trace.getFinishedAt(), trace.getStartedAt(), LocalDateTime.now());
        return AnalyticsEventTraceDO.builder()
                .tenantId(defaultTenant(trace.getTenantId()))
                .executionId(trace.getExecutionId())
                .nodeId(trace.getNodeId())
                .nodeType(trace.getNodeType())
                .nodeName(trace.getNodeName())
                .status(trace.getStatus() == null ? null : String.valueOf(trace.getStatus()))
                .inputJson(trace.getInputData())
                .outputJson(trace.getOutputData())
                .errorMessage(trace.getErrorMsg())
                .startedAt(trace.getStartedAt())
                .finishedAt(trace.getFinishedAt())
                .durationMs(trace.getDurationMs())
                .eventTime(eventTime)
                .receivedAt(LocalDateTime.now())
                .schemaVersion(1)
                .retentionClass("STANDARD")
                .archiveStatus("ACTIVE")
                .legalHold(false)
                .build();
    }

    private AnalyticsEventDO defaultEventFields(AnalyticsEventDO event) {
        LocalDateTime now = LocalDateTime.now();
        if (event.getTenantId() == null) {
            event.setTenantId(0L);
        }
        if (event.getEventTime() == null) {
            event.setEventTime(now);
        }
        if (event.getReceivedAt() == null) {
            event.setReceivedAt(now);
        }
        if (event.getSchemaVersion() == null) {
            event.setSchemaVersion(1);
        }
        if (event.getRetentionClass() == null || event.getRetentionClass().isBlank()) {
            event.setRetentionClass("STANDARD");
        }
        if (event.getArchiveStatus() == null || event.getArchiveStatus().isBlank()) {
            event.setArchiveStatus("ACTIVE");
        }
        if (event.getLegalHold() == null) {
            event.setLegalHold(false);
        }
        return event;
    }

    private Long defaultTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
