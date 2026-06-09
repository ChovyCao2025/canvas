package org.chovy.canvas.engine.trigger;

import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.infrastructure.mq.OverflowRetryMessage;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Owns trigger admission decisions before an execution context is created or resumed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerAdmissionService {

    private final ContextPersistenceService ctxStore;
    private final TriggerPreCheckService preCheckService;
    private final InFlightExecutionRegistry executionRegistry;
    private final TriggerPriorityConfig priorityConfig;
    private final ExecutionLaneResolver executionLaneResolver;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final CanvasExecutionMapper executionMapper;
    private final CanvasExecutionDlqMapper dlqMapper;
    private final Snowflake snowflake;

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    AdmissionDecision evaluate(AdmissionRequest request) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        boolean internalContinuation = isInternalContinuationTrigger(request.triggerType());
        boolean scheduledBatch = Boolean.TRUE.equals(payload.get(MapFieldKeys.SCHEDULED_BATCH));
        boolean quotaBypass = internalContinuation || scheduledBatch;
        ExecutionLane executionLane = request.executionLaneOverride() != null
                ? request.executionLaneOverride()
                : executionLaneResolver.resolve(
                        request.triggerType(),
                        request.triggerNodeType(),
                        payload,
                        request.overflowRetry(),
                        request.persistentRequest(),
                        request.priorAttemptCount());

        boolean isResume = shouldResumeExistingContext(
                request.canvasId(), request.userId(), request.dryRun(), internalContinuation);
        if (!request.dryRun() && internalContinuation && !isResume) {
            markMissingResumeContext(payload, request.msgId());
            return AdmissionDecision.shortCircuited(
                    internalContinuation,
                    quotaBypass,
                    false,
                    executionLane,
                    Map.of(
                            MapFieldKeys.SKIPPED, "missing-context",
                            MapFieldKeys.ERROR, "internal continuation context is missing"));
        }

        DedupResult dedup = performDedupCheck(
                request.canvasId(),
                request.userId(),
                request.msgId(),
                isResume,
                request.dryRun(),
                request.overflowRetry(),
                request.persistentRequest(),
                internalContinuation,
                request.globalTimeoutSec());
        if (dedup.deduplicated()) {
            return AdmissionDecision.shortCircuited(
                    internalContinuation,
                    quotaBypass,
                    isResume,
                    executionLane,
                    Map.of(MapFieldKeys.DEDUPLICATED, true));
        }
        String acquiredDedupKey = dedup.dedupKey();

        if (!request.dryRun() && !internalContinuation && !scheduledBatch) {
            preCheckService.checkWithoutQuotaAccounting(request.canvas(), request.userId());
            if (request.overflowRetry()) {
                log.debug("[ENGINE] 溢出重试跳过配额扣减（扣减在 doExecute 中进行）canvasId={} userId={}",
                        request.canvasId(), request.userId());
            }
        }

        int admissionLimit = request.globalMaxConcurrency();
        if (!request.dryRun()) {
            AdmissionResult admission = resolveAdmission(
                    request.canvasId(),
                    request.userId(),
                    request.triggerType(),
                    request.triggerNodeType(),
                    request.matchKey(),
                    payload,
                    request.msgId(),
                    request.canvas(),
                    request.overflowChainRetryCount(),
                    request.persistentRequest(),
                    request.globalMaxConcurrency());
            if (admission.isOverflow()) {
                releaseDedupIfPresent(acquiredDedupKey);
                return AdmissionDecision.shortCircuited(
                        internalContinuation,
                        quotaBypass,
                        isResume,
                        executionLane,
                        admission.overflowResponse());
            }
            admissionLimit = admission.admissionLimit();
        }

        // 汇总前面计算出的状态和明细，返回给调用方。
        return AdmissionDecision.granted(
                internalContinuation,
                quotaBypass,
                isResume,
                admissionLimit,
                acquiredDedupKey,
                executionLane);
    }

    /**
     * Only WAIT/approval/timeout style internal continuations may resume the Redis context.
     */
    boolean shouldResumeExistingContext(Long canvasId, String userId,
                                        boolean dryRun, boolean internalContinuation) {
        return !dryRun && internalContinuation && ctxStore.exists(canvasId, userId);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param triggerType 类型标识，用于选择对应处理分支。
     * @return 返回布尔判断结果。
     */
    static boolean isInternalContinuationTrigger(String triggerType) {
        return TriggerType.WAIT_RESUME.equals(triggerType)
                || TriggerType.WAIT_TIMEOUT.equals(triggerType)
                || TriggerType.APPROVAL_RESUME.equals(triggerType)
                || TriggerType.APPROVAL_TIMEOUT.equals(triggerType)
                || TriggerType.HUB_TIMEOUT.equals(triggerType)
                || TriggerType.AGGREGATE_TIMEOUT.equals(triggerType)
                || TriggerType.THRESHOLD_TIMEOUT.equals(triggerType);
    }

    /** Sends an overflowed trigger request to the delayed retry topic. */
    boolean enqueueOverflowRetry(Long canvasId, String userId, String triggerType,
                                 String triggerNodeType, String matchKey,
                                 Map<String, Object> payload, String msgId, int chainRetryCount) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> retryPayload = sanitizePayload(payload);
        try {
            String effectiveMsgId = nonBlank(msgId)
                    ? msgId
                    : "overflow-" + snowflake.nextIdStr();
            OverflowRetryMessage msg = new OverflowRetryMessage(
                    canvasId, userId,
                    triggerType, triggerNodeType,
                    matchKey, retryPayload,
                    effectiveMsgId, chainRetryCount + 1);
            String tag = triggerType != null ? triggerType : TriggerPriorityConfig.Priority.NORMAL.name();
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            Message rocketMsg = new Message(
                    "CANVAS_TRIGGER_OVERFLOW",
                    tag,
                    objectMapper.writeValueAsBytes(msg)
            );
            rocketMsg.setDeliverTimeMs(System.currentTimeMillis() + priorityConfig.getOverflowRetryDelayMs());
            SendResult sendResult = rocketMQTemplate.getProducer().send(rocketMsg);
            if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
                log.error("[ENGINE] 溢出消息发送未确认成功 canvasId={} status={}",
                        canvasId, sendResult == null ? null : sendResult.getSendStatus());
                writeOverflowEnqueueDlq(msg, "overflow_retry_enqueue_failed:"
                        + (sendResult == null ? "null" : sendResult.getSendStatus()));
                return false;
            }
            log.info("[ENGINE] 溢出事件入队重试 canvasId={} userId={} retryCount={}",
                    canvasId, userId, chainRetryCount + 1);
            return true;
        } catch (Exception e) {
            log.error("[ENGINE] 溢出消息发送失败，事件丢失 canvasId={}: {}", canvasId, e.getMessage());
            OverflowRetryMessage msg = new OverflowRetryMessage(
                    canvasId, userId, triggerType, triggerNodeType,
                    matchKey, retryPayload, nonBlank(msgId) ? msgId : "overflow-" + UUID.randomUUID(),
                    chainRetryCount + 1);
            writeOverflowEnqueueDlq(msg, "overflow_retry_enqueue_failed:" + e.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return false;
        }
    }

    /** Sanitizes internal retry control fields from business payloads. */
    Map<String, Object> sanitizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        java.util.HashMap<String, Object> sanitized = new java.util.HashMap<>(payload);
        sanitized.remove(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
        return sanitized;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param msgId 业务对象 ID，用于定位具体记录。
     * @param isResume is resume 参数，用于 performDedupCheck 流程中的校验、计算或对象转换。
     * @param dryRun dry run 参数，用于 performDedupCheck 流程中的校验、计算或对象转换。
     * @param overflowRetry overflow retry 参数，用于 performDedupCheck 流程中的校验、计算或对象转换。
     * @param persistentRequest 请求对象，承载本次操作的输入参数。
     * @param internalContinuation internal continuation 参数，用于 performDedupCheck 流程中的校验、计算或对象转换。
     * @param globalTimeoutSec 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 performDedupCheck 流程生成的业务结果。
     */
    private DedupResult performDedupCheck(Long canvasId, String userId, String msgId,
                                          boolean isResume, boolean dryRun, boolean overflowRetry,
                                          boolean persistentRequest, boolean internalContinuation,
                                          long globalTimeoutSec) {
        if (dryRun || overflowRetry || persistentRequest || internalContinuation || msgId == null) {
            return DedupResult.skipped();
        }
        Duration dedupTtl = isResume
                ? Duration.ofSeconds(globalTimeoutSec + 600)
                : Duration.ofHours(24);
        if (!ctxStore.acquireDedup(canvasId, userId, msgId, dedupTtl)) {
            log.debug("[ENGINE] dedup 拦截 canvasId={} userId={} msgId={}", canvasId, userId, msgId);
            return DedupResult.duplicate();
        }
        return DedupResult.acquired(ctxStore.buildDedupKey(canvasId, userId, msgId));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param triggerType 类型标识，用于选择对应处理分支。
     * @param triggerNodeType 类型标识，用于选择对应处理分支。
     * @param matchKey 业务键，用于在同一租户下定位资源。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param msgId 业务对象 ID，用于定位具体记录。
     * @param canvas canvas 参数，用于 resolveAdmission 流程中的校验、计算或对象转换。
     * @param overflowChainRetryCount overflow chain retry count 参数，用于 resolveAdmission 流程中的校验、计算或对象转换。
     * @param persistentRequest 请求对象，承载本次操作的输入参数。
     * @param globalMaxConcurrency global max concurrency 参数，用于 resolveAdmission 流程中的校验、计算或对象转换。
     * @return 返回 resolveAdmission 流程生成的业务结果。
     */
    private AdmissionResult resolveAdmission(
            Long canvasId, String userId, String triggerType,
            String triggerNodeType, String matchKey,
            Map<String, Object> payload, String msgId,
            CanvasDO canvas, int overflowChainRetryCount, boolean persistentRequest,
            int globalMaxConcurrency) {
        // 准备本次处理所需的上下文和中间变量。
        TriggerPriorityConfig.Priority priority = priorityConfig.of(triggerType);
        int active = executionRegistry.activeCount(canvasId);
        int maxConc = globalMaxConcurrency;

        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (priority == TriggerPriorityConfig.Priority.HIGH) {
            int highMax = Math.max(1, (int) (maxConc * priorityConfig.getHighMaxConcurrencyRatio()));
            if (active >= highMax) {
                log.error("[ENGINE] HIGH优先级超并发告警 canvasId={} active={}/{}",
                        canvasId, active, highMax);
            }
            return AdmissionResult.granted(globalMaxConcurrency);
        }

        int effectiveMax = priority == TriggerPriorityConfig.Priority.LOW
                ? Math.max(1, (int) (maxConc * priorityConfig.getLowRatio()))
                : maxConc;

        if (active >= effectiveMax) {
            log.warn("[ENGINE] 并发上限 canvasId={} active={}/{} priority={}",
                    canvasId, active, effectiveMax, priority);
            if (priority == TriggerPriorityConfig.Priority.NORMAL) {
                if (persistentRequest) {
                    return AdmissionResult.overflow("request_retry");
                }
                if (overflowChainRetryCount >= priorityConfig.getOverflowMaxRetry()) {
                    log.error("[ENGINE] 溢出重试次数达上限 canvasId={} userId={} retryCount={}/{}，写 DLQ",
                            canvasId, userId, overflowChainRetryCount, priorityConfig.getOverflowMaxRetry());
                    String effectiveMsgId = nonBlank(msgId) ? msgId : "overflow-max-" + UUID.randomUUID();
                    writeOverflowEnqueueDlq(
                            new OverflowRetryMessage(
                                    canvasId, userId, triggerType, triggerNodeType,
                                    matchKey, sanitizePayload(payload), effectiveMsgId,
                                    overflowChainRetryCount),
                            "max_retry_exceeded:" + overflowChainRetryCount);
                    return AdmissionResult.overflow("max_retry_exceeded");
                }
                boolean queued = enqueueOverflowRetry(canvasId, userId, triggerType, triggerNodeType,
                        matchKey, payload, msgId, overflowChainRetryCount);
                return AdmissionResult.overflow(queued ? "queued_for_retry" : "retry_enqueue_failed");
            }
            return AdmissionResult.overflow("dropped_low_priority");
        }

        // 汇总前面计算出的状态和明细，返回给调用方。
        return AdmissionResult.granted(effectiveMax);
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param acquiredDedupKey 业务键，用于在同一租户下定位资源。
     */
    private void releaseDedupIfPresent(String acquiredDedupKey) {
        if (acquiredDedupKey != null) {
            ctxStore.releaseDedup(acquiredDedupKey);
        }
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param msgId 业务对象 ID，用于定位具体记录。
     */
    void markMissingResumeContext(Map<String, Object> payload, String msgId) {
        String executionId = resolveContinuationExecutionId(payload, msgId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (executionId == null || executionId.isBlank()) {
            log.warn("[ENGINE] internal continuation context missing, but executionId is unavailable");
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CanvasExecutionDO update = new CanvasExecutionDO();
        update.setStatus(ExecutionStatus.FAILED.getCode());
        try {
            update.setResult(objectMapper.writeValueAsString(Map.of(
                    MapFieldKeys.ERROR, "MISSING_EXECUTION_CONTEXT",
                    MapFieldKeys.REASON_MESSAGE, "Redis execution context is missing for internal continuation"
            )));
        } catch (Exception ignored) {
        }
        int updated = executionMapper.update(update,
                new LambdaUpdateWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getId, executionId)
                        .eq(CanvasExecutionDO::getStatus, ExecutionStatus.PAUSED.getCode()));
        if (updated == 0) {
            log.warn("[ENGINE] missing context mark failed, execution may no longer be PAUSED executionId={}",
                    executionId);
        } else {
            log.error("[ENGINE] missing Redis context, marked execution FAILED executionId={}", executionId);
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param MapString map string 参数，用于 resolveContinuationExecutionId 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param msgId 业务对象 ID，用于定位具体记录。
     * @return 返回 resolve continuation execution id 生成的文本或业务键。
     */
    private String resolveContinuationExecutionId(Map<String, Object> payload, String msgId) {
        // 准备本次处理所需的上下文和中间变量。
        Object payloadExecutionId = payload == null ? null : payload.get(MapFieldKeys.EXECUTION_ID);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (payloadExecutionId instanceof String text && !text.isBlank()) {
            return text;
        }
        Object sourceExecutionId = payload == null ? null : payload.get(MapFieldKeys.SOURCE_EXECUTION_ID);
        if (sourceExecutionId instanceof String text && !text.isBlank()) {
            return text;
        }
        if (msgId == null || msgId.isBlank()) {
            return null;
        }
        int waitMarker = msgId.indexOf(":wait:");
        if (waitMarker > 0) {
            return msgId.substring(0, waitMarker);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param msg msg 参数，用于 writeOverflowEnqueueDlq 流程中的校验、计算或对象转换。
     * @param errorMsg error msg 参数，用于 writeOverflowEnqueueDlq 流程中的校验、计算或对象转换。
     */
    private void writeOverflowEnqueueDlq(OverflowRetryMessage msg, String errorMsg) {
        // 准备本次处理所需的上下文和中间变量。
        try {
            CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
                    .executionId(msg.getMsgId())
                    .canvasId(msg.getCanvasId())
                    .userId(msg.getUserId())
                    .perfRunId(PerfRunContext.extract(msg.getPayload()))
                    .failedNodeId("OVERFLOW_ENQUEUE")
                    .failedNodeType(msg.getTriggerNodeType())
                    .errorMsg(truncate(errorMsg, 500))
                    .retryCount(msg.getChainRetryCount())
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    .triggerPayload(objectMapper.writeValueAsString(msg.getPayload()))
                    .triggerType(msg.getTriggerType())
                    .triggerNodeType(msg.getTriggerNodeType())
                    .matchKey(msg.getMatchKey())
                    .failedAt(LocalDateTime.now())
                    .build();
            dlqMapper.insert(dlq);
        } catch (Exception e) {
            log.error("[ENGINE] 溢出入队失败写DLQ失败 canvasId={} msgId={}: {}",
                    msg.getCanvasId(), msg.getMsgId(), e.getMessage());
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 non blank 的布尔判断结果。
     */
    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 truncate 流程中的校验、计算或对象转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), maxLength));
    }

    /**
     * AdmissionRequest 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record AdmissionRequest(
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String msgId,
            boolean dryRun,
            boolean overflowRetry,
            int overflowChainRetryCount,
            boolean persistentRequest,
            int priorAttemptCount,
            CanvasDO canvas,
            int globalMaxConcurrency,
            long globalTimeoutSec,
            ExecutionLane executionLaneOverride) {
    }

    /**
     * AdmissionDecision 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record AdmissionDecision(
            boolean internalContinuation,
            boolean quotaBypass,
            boolean isResume,
            int admissionLimit,
            String dedupKey,
            ExecutionLane executionLane,
            Map<String, Object> shortCircuitResponse) {

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param internalContinuation internal continuation 参数，用于 granted 流程中的校验、计算或对象转换。
         * @param quotaBypass quota bypass 参数，用于 granted 流程中的校验、计算或对象转换。
         * @param isResume is resume 参数，用于 granted 流程中的校验、计算或对象转换。
         * @param admissionLimit admission limit 参数，用于 granted 流程中的校验、计算或对象转换。
         * @param dedupKey 业务键，用于在同一租户下定位资源。
         * @param executionLane execution lane 参数，用于 granted 流程中的校验、计算或对象转换。
         * @return 返回 granted 流程生成的业务结果。
         */
        static AdmissionDecision granted(boolean internalContinuation,
                                         boolean quotaBypass,
                                         boolean isResume,
                                         int admissionLimit,
                                         String dedupKey,
                                         ExecutionLane executionLane) {
            return new AdmissionDecision(
                    internalContinuation,
                    quotaBypass,
                    isResume,
                    admissionLimit,
                    dedupKey,
                    executionLane,
                    null);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param internalContinuation internal continuation 参数，用于 shortCircuited 流程中的校验、计算或对象转换。
         * @param quotaBypass quota bypass 参数，用于 shortCircuited 流程中的校验、计算或对象转换。
         * @param isResume is resume 参数，用于 shortCircuited 流程中的校验、计算或对象转换。
         * @param executionLane execution lane 参数，用于 shortCircuited 流程中的校验、计算或对象转换。
         * @param response response 参数，用于 shortCircuited 流程中的校验、计算或对象转换。
         * @return 返回 shortCircuited 流程生成的业务结果。
         */
        static AdmissionDecision shortCircuited(boolean internalContinuation,
                                               boolean quotaBypass,
                                               boolean isResume,
                                               ExecutionLane executionLane,
                                               Map<String, Object> response) {
            return new AdmissionDecision(
                    internalContinuation,
                    quotaBypass,
                    isResume,
                    0,
                    null,
                    executionLane,
                    response);
        }

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @return 返回布尔判断结果。
         */
        boolean isShortCircuited() {
            return shortCircuitResponse != null;
        }
    }

    /**
     * DedupResult 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    private record DedupResult(boolean deduplicated, String dedupKey) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 duplicate 流程生成的业务结果。
         */
        static DedupResult duplicate() {
            return new DedupResult(true, null);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param key 业务键，用于在同一租户下定位资源。
         * @return 返回 acquired 流程生成的业务结果。
         */
        static DedupResult acquired(String key) {
            return new DedupResult(false, key);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 skipped 流程生成的业务结果。
         */
        static DedupResult skipped() {
            return new DedupResult(false, null);
        }
    }

    /**
     * AdmissionResult 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    private record AdmissionResult(int admissionLimit, Map<String, Object> overflowResponse) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param limit 分页或数量限制，避免一次处理过多数据。
         * @return 返回 granted 流程生成的业务结果。
         */
        static AdmissionResult granted(int limit) {
            return new AdmissionResult(limit, null);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param code 业务编码，用于匹配对应类型或状态。
         * @return 返回 overflow 流程生成的业务结果。
         */
        static AdmissionResult overflow(String code) {
            return new AdmissionResult(0, Map.of(MapFieldKeys.OVERFLOW, code));
        }

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @return 返回布尔判断结果。
         */
        boolean isOverflow() {
            return overflowResponse != null;
        }
    }
}
