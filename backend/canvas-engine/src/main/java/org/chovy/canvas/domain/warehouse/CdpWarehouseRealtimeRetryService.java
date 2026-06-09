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
/**
 * CdpWarehouseRealtimeRetryService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseRealtimeRetryService 实例。
     *
     * @param retryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventSink event sink 参数，用于 CdpWarehouseRealtimeRetryService 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseRealtimeRetryService(CdpWarehouseRealtimeRetryMapper retryMapper,
                                            CdpEventLogMapper eventLogMapper,
                                            CdpWarehouseEventSink eventSink) {
        this(retryMapper, eventLogMapper, eventSink, null);
    }

    @Autowired
    /**
     * 初始化 CdpWarehouseRealtimeRetryService 实例。
     *
     * @param retryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventSink event sink 参数，用于 CdpWarehouseRealtimeRetryService 流程中的校验、计算或对象转换。
     * @param checkpointService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param errorMessage error message 参数，用于 enqueueFailure 流程中的校验、计算或对象转换。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param maxAttempts max attempts 参数，用于 retryDue 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public RetryResult retryDue(LocalDateTime now, int limit, int maxAttempts) {
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        int boundedLimit = normalizeLimit(limit);
        int attempts = Math.max(maxAttempts, 1);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RetryResult(success, retried, dead, skipped);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param event event 参数，用于 recordDelivered 流程中的校验、计算或对象转换。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param event event 参数，用于 recordFailure 流程中的校验、计算或对象转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param attempt attempt 参数，用于 nextRetryAt 流程中的校验、计算或对象转换。
     * @return 返回 nextRetryAt 流程生成的业务结果。
     */
    private LocalDateTime nextRetryAt(LocalDateTime now, int attempt) {
        long delaySeconds = Math.min(300L, 10L * Math.max(attempt, 1));
        return now.plusSeconds(delaySeconds);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 safe attempt count 计算得到的数量、金额或指标值。
     */
    private int safeAttemptCount(CdpWarehouseRealtimeRetryDO row) {
        return row.getAttemptCount() == null ? 0 : row.getAttemptCount();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String message) {
        String value = message == null ? "warehouse realtime retry failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * RetryResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RetryResult(int success, int retried, int dead, int skipped) {
    }
}
