package org.chovy.canvas.engine.request;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneResolver;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.TriggerPriorityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 画布执行请求 Dispatcher 执行请求治理组件。
 *
 * <p>负责画布执行请求的排队、派发、限流、重放或积压度量，削峰高并发触发流量。
 * <p>该层位于触发入口和 DAG 执行之间，核心目标是保护执行引擎稳定性。
 */
@Slf4j
@Service
public class CanvasExecutionRequestDispatcher {

    /** 执行请求 Mapper，用于抢占到期请求和更新状态。 */
    private final CanvasExecutionRequestMapper mapper;
    /** Disruptor 派发服务，承接执行请求异步消费。 */
    private final CanvasDisruptorService disruptorService;
    /** 画布指标埋点器。 */
    private final CanvasMetrics metrics;
    /** 触发优先级配置，用于计算单画布派发额度。 */
    private final TriggerPriorityConfig priorityConfig;
    /** 每轮最多捞取的执行请求数量。 */
    private final int batchSize;
    /** RUNNING 请求判定为陈旧可重派发的秒数。 */
    private final long runningStaleSeconds;
    /** 单个画布在一轮派发中的基础额度。 */
    private final int perCanvasBatchLimit;
    /** 是否启用单画布派发额度自适应调整。 */
    private final boolean adaptivePerCanvasLimitEnabled;
    /** 热点画布判定阈值相对基础额度的倍数。 */
    private final int adaptiveHotCanvasThresholdMultiplier;
    /** 热点画布额度收缩百分比。 */
    private final int adaptiveHotCanvasReductionPercent;
    /** 非热点画布的额外派发额度。 */
    private final int adaptiveIdleCanvasBonus;
    /** 高优先级画布的额外派发额度。 */
    private final int adaptiveHighPriorityCanvasBonus;
    /** 执行 lane 解析器，用于请求派发前按 lane 做隔离。 */
    private final ExecutionLaneResolver laneResolver;
    /** 是否启用请求派发 lane 隔离。 */
    private final boolean laneIsolationEnabled;

    @Autowired
    public CanvasExecutionRequestDispatcher(CanvasExecutionRequestMapper mapper,
                                            CanvasDisruptorService disruptorService,
                                            CanvasMetrics metrics,
                                            TriggerPriorityConfig priorityConfig,
                                            @Value("${canvas.execution-request.dispatch-batch-size:200}") int batchSize,
                                            @Value("${canvas.execution-request.running-stale-sec:300}") long runningStaleSeconds,
                                            @Value("${canvas.execution-request.per-canvas-batch-limit:0}") int perCanvasBatchLimit,
                                            @Value("${canvas.execution-request.adaptive-per-canvas-limit-enabled:true}") boolean adaptivePerCanvasLimitEnabled,
                                            @Value("${canvas.execution-request.adaptive-hot-canvas-threshold-multiplier:2}") int adaptiveHotCanvasThresholdMultiplier,
                                            @Value("${canvas.execution-request.adaptive-hot-canvas-reduction-percent:50}") int adaptiveHotCanvasReductionPercent,
                                            @Value("${canvas.execution-request.adaptive-idle-canvas-bonus:1}") int adaptiveIdleCanvasBonus,
                                            @Value("${canvas.execution-request.adaptive-high-priority-canvas-bonus:2}") int adaptiveHighPriorityCanvasBonus,
                                            ExecutionLaneResolver laneResolver,
                                            @Value("${canvas.execution-request.lane-isolation.enabled:false}") boolean laneIsolationEnabled) {
        this.mapper = mapper;
        this.disruptorService = disruptorService;
        this.metrics = metrics;
        this.priorityConfig = priorityConfig;
        this.batchSize = batchSize;
        this.runningStaleSeconds = runningStaleSeconds;
        this.perCanvasBatchLimit = normalizePerCanvasLimit(perCanvasBatchLimit);
        this.adaptivePerCanvasLimitEnabled = adaptivePerCanvasLimitEnabled;
        this.adaptiveHotCanvasThresholdMultiplier = Math.max(1, adaptiveHotCanvasThresholdMultiplier);
        this.adaptiveHotCanvasReductionPercent = Math.max(1, Math.min(100, adaptiveHotCanvasReductionPercent));
        this.adaptiveIdleCanvasBonus = Math.max(0, adaptiveIdleCanvasBonus);
        this.adaptiveHighPriorityCanvasBonus = Math.max(0, adaptiveHighPriorityCanvasBonus);
        this.laneResolver = laneResolver;
        this.laneIsolationEnabled = laneIsolationEnabled;
    }

    @Scheduled(fixedDelayString = "${canvas.execution-request.dispatch-fixed-delay-ms:1000}")
    public void dispatchDueRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(runningStaleSeconds);
        // 只捞取到期请求和超时 RUNNING 请求，避免把正在健康执行的记录重复派发。
        List<CanvasExecutionRequestDO> requests = mapper.selectDueRequests(batchSize, now, staleBefore);
        Map<Long, Integer> canvasCounts = new HashMap<>();
        Map<Long, CanvasDispatchProfile> profiles = buildDispatchProfiles(requests);
        for (CanvasExecutionRequestDO request : requests) {
            if (request == null || request.getId() == null) {
                continue;
            }
            // 单画布批次上限用于给热点画布做局部回压，防止单个租户吃掉整个派发窗口。
            if (exceedsPerCanvasLimit(request, canvasCounts, profiles)) {
                continue;
            }
            try {
                boolean dispatched = dispatchRequest(request);
                if (!dispatched) {
                    recordSkipped(request.getCanvasId(), "lane_worker_capacity");
                }
            } catch (RuntimeException e) {
                // 发布失败直接停止本轮派发，保留剩余请求给下一次调度重试。
                recordDispatchFailure(request.getCanvasId());
                log.warn("[EXEC_REQUEST] dispatch stopped requestId={} reason={}", request.getId(), e.getMessage());
                return;
            }
        }
    }

    private boolean dispatchRequest(CanvasExecutionRequestDO request) {
        if (!laneIsolationEnabled) {
            publishRequest(request);
            return true;
        }
        ExecutionLane lane = resolveLane(request);
        publishRequest(request, lane);
        return true;
    }

    private void publishRequest(CanvasExecutionRequestDO request) {
        // 先发布到 Disruptor，再由 worker 池消费执行，派发线程不直接进入 DAG。
        disruptorService.publishRequest(request.getId());
        recordDispatched(request.getCanvasId());
    }

    private void publishRequest(CanvasExecutionRequestDO request, ExecutionLane lane) {
        disruptorService.publishRequest(request.getId(), lane);
        recordDispatched(request.getCanvasId());
    }

    private ExecutionLane resolveLane(CanvasExecutionRequestDO request) {
        if (laneResolver == null) {
            return ExecutionLane.STANDARD;
        }
        int attemptCount = request.getAttemptCount() == null ? 0 : request.getAttemptCount();
        return laneResolver.resolve(
                request.getTriggerType(),
                request.getTriggerNodeType(),
                Map.of(),
                false,
                true,
                attemptCount);
    }

    /**
     * 执行 exceeds Per Canvas Limit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param request 请求对象，承载调用方提交的业务参数
     * @param canvasCounts canvasCounts 画布相关对象或标识
     * @param profiles profiles 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean exceedsPerCanvasLimit(CanvasExecutionRequestDO request,
                                          Map<Long, Integer> canvasCounts,
                                          Map<Long, CanvasDispatchProfile> profiles) {
        Long canvasId = request.getCanvasId();
        if (perCanvasBatchLimit == Integer.MAX_VALUE || canvasId == null) {
            return false;
        }
        int count = canvasCounts.getOrDefault(canvasId, 0);
        CanvasDispatchProfile profile = profiles.get(canvasId);
        int effectiveLimit = effectivePerCanvasLimit(profile);
        if (count >= effectiveLimit) {
            // 当前画布已达到本轮可派发额度，跳过后续请求让出调度窗口。
            recordSkipped(canvasId, skipReason(profile, effectiveLimit));
            return true;
        }
        canvasCounts.put(canvasId, count + 1);
        return false;
    }

    /**
     * 构建、解析或转换 build Dispatch Profiles 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param requests requests 方法执行所需的业务参数
     * @return 按业务键组织的映射结果
     */
    private Map<Long, CanvasDispatchProfile> buildDispatchProfiles(List<CanvasExecutionRequestDO> requests) {
        Map<Long, CanvasDispatchProfile> profiles = new HashMap<>();
        for (CanvasExecutionRequestDO request : requests) {
            if (request == null || request.getCanvasId() == null) {
                continue;
            }
            // 同一个画布汇总优先级和请求数，用来决定是否触发自适应限流。
            profiles.computeIfAbsent(request.getCanvasId(), ignored -> new CanvasDispatchProfile())
                    .register(priorityOf(request.getTriggerType()));
        }
        return profiles;
    }

    private TriggerPriorityConfig.Priority priorityOf(String triggerType) {
        return priorityConfig != null ? priorityConfig.of(triggerType) : TriggerPriorityConfig.Priority.NORMAL;
    }

    /**
     * 执行 effective Per Canvas Limit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param profile profile 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int effectivePerCanvasLimit(CanvasDispatchProfile profile) {
        if (perCanvasBatchLimit == Integer.MAX_VALUE || profile == null) {
            return perCanvasBatchLimit;
        }
        int base = perCanvasBatchLimit;
        if (!adaptivePerCanvasLimitEnabled) {
            return base;
        }
        int hotThreshold = Math.max(base + 1, base * adaptiveHotCanvasThresholdMultiplier);
        if (profile.totalRequests >= hotThreshold && profile.highestPriority != TriggerPriorityConfig.Priority.HIGH) {
            return Math.max(1, (int) Math.floor(base * (adaptiveHotCanvasReductionPercent / 100.0)));
        }
        if (profile.highestPriority == TriggerPriorityConfig.Priority.HIGH) {
            return Math.min(batchSize, base + adaptiveHighPriorityCanvasBonus);
        }
        return Math.min(batchSize, base + adaptiveIdleCanvasBonus);
    }

    /**
     * 执行 skip Reason 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param profile profile 方法执行所需的业务参数
     * @param effectiveLimit effectiveLimit 数量、阈值或分页参数
     * @return 转换或查询得到的字符串结果
     */
    private String skipReason(CanvasDispatchProfile profile, int effectiveLimit) {
        if (profile == null || !adaptivePerCanvasLimitEnabled) {
            return "per_canvas_batch_limit";
        }
        int hotThreshold = Math.max(perCanvasBatchLimit + 1, perCanvasBatchLimit * adaptiveHotCanvasThresholdMultiplier);
        if (profile.totalRequests >= hotThreshold && profile.highestPriority != TriggerPriorityConfig.Priority.HIGH
                && effectiveLimit < perCanvasBatchLimit) {
            return "adaptive_hot_canvas_limit";
        }
        return "per_canvas_batch_limit";
    }

    /**
     * 执行 normalize Per Canvas Limit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 计算得到的数值结果
     */
    private int normalizePerCanvasLimit(int value) {
        return value <= 0 ? Integer.MAX_VALUE : value;
    }

    /**
     * 写入或记录 record Dispatched 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     */
    private void recordDispatched(Long canvasId) {
        if (metrics != null) {
            metrics.recordExecutionRequestDispatched(normalizeCanvasId(canvasId));
        }
    }

    /**
     * 写入或记录 record Skipped 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param reason reason 方法执行所需的业务参数
     */
    private void recordSkipped(Long canvasId, String reason) {
        if (metrics != null) {
            metrics.recordExecutionRequestSkipped(normalizeCanvasId(canvasId), reason);
        }
    }

    /**
     * 写入或记录 record Dispatch Failure 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     */
    private void recordDispatchFailure(Long canvasId) {
        if (metrics != null) {
            metrics.recordExecutionRequestDispatchFailure(normalizeCanvasId(canvasId));
        }
    }

    /**
     * 执行 normalize Canvas Id 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
    private String normalizeCanvasId(Long canvasId) {
        return canvasId != null ? String.valueOf(canvasId) : "UNKNOWN";
    }

    private static final class CanvasDispatchProfile {
        /** 当前画布在本轮派发窗口内的请求数量。 */
        private int totalRequests;
        /** 当前画布在本轮派发窗口内观测到的最高优先级。 */
        private TriggerPriorityConfig.Priority highestPriority = TriggerPriorityConfig.Priority.LOW;

        /**
         * 注册、调度或初始化 register 相关的业务数据。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param priority priority 方法执行所需的业务参数
         */
        private void register(TriggerPriorityConfig.Priority priority) {
            totalRequests++;
            if (priority == TriggerPriorityConfig.Priority.HIGH
                    || (priority == TriggerPriorityConfig.Priority.NORMAL
                    && highestPriority == TriggerPriorityConfig.Priority.LOW)) {
                highestPriority = priority;
            }
        }
    }
}
