package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CdpWarehouseRealtimeRetryService {

    private static final int MAX_LIMIT = 500;
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RETRY = "RETRY";

    private final CdpWarehouseRealtimeRetryMapper retryMapper;
    private final CdpEventLogMapper eventLogMapper;
    private final CdpWarehouseEventSink eventSink;
    private final CdpWarehouseRealtimeCheckpointService checkpointService;
    private final String workerId;

    public CdpWarehouseRealtimeRetryService(CdpWarehouseRealtimeRetryMapper retryMapper,
                                            CdpEventLogMapper eventLogMapper,
                                            CdpWarehouseEventSink eventSink) {
        this(retryMapper, eventLogMapper, eventSink, null);
    }

    @Autowired
    public CdpWarehouseRealtimeRetryService(CdpWarehouseRealtimeRetryMapper retryMapper,
                                            CdpEventLogMapper eventLogMapper,
                                            CdpWarehouseEventSink eventSink,
                                            CdpWarehouseRealtimeCheckpointService checkpointService) {
        this.retryMapper = retryMapper;
        this.eventLogMapper = eventLogMapper;
        this.eventSink = eventSink;
        this.checkpointService = checkpointService;
        this.workerId = "warehouse-retry-" + UUID.randomUUID();
    }

    public void enqueueFailure(CdpEventLogDO row, String errorMessage) {
        if (row == null || row.getId() == null) {
            return;
        }
        CdpWarehouseRealtimeRetryDO retry = new CdpWarehouseRealtimeRetryDO();
        retry.setTenantId(normalizeTenant(row.getTenantId()));
        retry.setEventLogId(row.getId());
        retry.setMessageId(row.getMessageId());
        retry.setEventCode(row.getEventCode());
        retry.setStatus(STATUS_PENDING);
        retry.setAttemptCount(0);
        retry.setFirstError(limit(errorMessage));
        retry.setLastError(limit(errorMessage));
        retry.setNextRetryAt(LocalDateTime.now());
        retryMapper.upsertPending(retry);
    }

    public RetryResult retryDue(LocalDateTime now, int limit, int maxAttempts) {
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        int boundedLimit = normalizeLimit(limit);
        int attempts = Math.max(maxAttempts, 1);
        List<CdpWarehouseRealtimeRetryDO> rows = retryMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO>()
                        .in(CdpWarehouseRealtimeRetryDO::getStatus, STATUS_PENDING, STATUS_RETRY)
                        .le(CdpWarehouseRealtimeRetryDO::getNextRetryAt, effectiveNow)
                        .orderByAsc(CdpWarehouseRealtimeRetryDO::getNextRetryAt)
                        .orderByAsc(CdpWarehouseRealtimeRetryDO::getId)
                        .last("LIMIT " + boundedLimit));

        int success = 0;
        int retried = 0;
        int dead = 0;
        int skipped = 0;
        for (CdpWarehouseRealtimeRetryDO row : rows == null ? List.<CdpWarehouseRealtimeRetryDO>of() : rows) {
            if (retryMapper.claimDue(row.getId(), workerId, effectiveNow) != 1) {
                skipped++;
                continue;
            }
            CdpEventLogDO event = eventLogMapper.selectById(row.getEventLogId());
            if (event == null) {
                retryMapper.markDead(row.getId(), "event log not found: " + row.getEventLogId(), effectiveNow);
                dead++;
                continue;
            }
            try {
                eventSink.writeAccepted(event);
                retryMapper.markSuccess(row.getId(), effectiveNow);
                recordDelivered(event);
                success++;
            } catch (RuntimeException ex) {
                int nextAttempt = safeAttemptCount(row) + 1;
                String message = limit(ex.getMessage());
                recordFailure(event, message);
                if (nextAttempt >= attempts) {
                    retryMapper.markDead(row.getId(), message, effectiveNow);
                    dead++;
                } else {
                    retryMapper.markRetry(row.getId(), message, nextRetryAt(effectiveNow, nextAttempt), effectiveNow);
                    retried++;
                }
            }
        }
        return new RetryResult(success, retried, dead, skipped);
    }

    private void recordDelivered(CdpEventLogDO event) {
        if (checkpointService == null) {
            return;
        }
        try {
            checkpointService.recordDelivered(event, "RETRY");
        } catch (RuntimeException ex) {
            log.warn("[CDP] warehouse retry checkpoint delivered update failed eventLogId={}: {}",
                    event.getId(), ex.getMessage());
        }
    }

    private void recordFailure(CdpEventLogDO event, String message) {
        if (checkpointService == null) {
            return;
        }
        try {
            checkpointService.recordFailure(event, message);
        } catch (RuntimeException ex) {
            log.warn("[CDP] warehouse retry checkpoint failure update failed eventLogId={}: {}",
                    event.getId(), ex.getMessage());
        }
    }

    private LocalDateTime nextRetryAt(LocalDateTime now, int attempt) {
        long delaySeconds = Math.min(300L, 10L * Math.max(attempt, 1));
        return now.plusSeconds(delaySeconds);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int safeAttemptCount(CdpWarehouseRealtimeRetryDO row) {
        return row.getAttemptCount() == null ? 0 : row.getAttemptCount();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String limit(String message) {
        String value = message == null ? "warehouse realtime retry failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    public record RetryResult(int success, int retried, int dead, int skipped) {
    }
}
