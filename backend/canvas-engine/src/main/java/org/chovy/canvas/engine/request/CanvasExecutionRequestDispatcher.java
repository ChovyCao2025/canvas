package org.chovy.canvas.engine.request;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
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

@Slf4j
@Service
public class CanvasExecutionRequestDispatcher {

    private final CanvasExecutionRequestMapper mapper;
    private final CanvasDisruptorService disruptorService;
    private final CanvasMetrics metrics;
    private final TriggerPriorityConfig priorityConfig;
    private final int batchSize;
    private final long runningStaleSeconds;
    private final int perCanvasBatchLimit;
    private final boolean adaptivePerCanvasLimitEnabled;
    private final int adaptiveHotCanvasThresholdMultiplier;
    private final int adaptiveHotCanvasReductionPercent;
    private final int adaptiveIdleCanvasBonus;
    private final int adaptiveHighPriorityCanvasBonus;

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
                                            @Value("${canvas.execution-request.adaptive-high-priority-canvas-bonus:2}") int adaptiveHighPriorityCanvasBonus) {
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
    }

    @Scheduled(fixedDelayString = "${canvas.execution-request.dispatch-fixed-delay-ms:1000}")
    public void dispatchDueRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(runningStaleSeconds);
        List<CanvasExecutionRequest> requests = mapper.selectDueRequests(batchSize, now, staleBefore);
        Map<Long, Integer> canvasCounts = new HashMap<>();
        Map<Long, CanvasDispatchProfile> profiles = buildDispatchProfiles(requests);
        for (CanvasExecutionRequest request : requests) {
            if (request == null || request.getId() == null) {
                continue;
            }
            if (exceedsPerCanvasLimit(request, canvasCounts, profiles)) {
                continue;
            }
            try {
                disruptorService.publishRequest(request.getId());
                recordDispatched(request.getCanvasId());
            } catch (RuntimeException e) {
                recordDispatchFailure(request.getCanvasId());
                log.warn("[EXEC_REQUEST] dispatch stopped requestId={} reason={}", request.getId(), e.getMessage());
                return;
            }
        }
    }

    private boolean exceedsPerCanvasLimit(CanvasExecutionRequest request,
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
            recordSkipped(canvasId, skipReason(profile, effectiveLimit));
            return true;
        }
        canvasCounts.put(canvasId, count + 1);
        return false;
    }

    private Map<Long, CanvasDispatchProfile> buildDispatchProfiles(List<CanvasExecutionRequest> requests) {
        Map<Long, CanvasDispatchProfile> profiles = new HashMap<>();
        for (CanvasExecutionRequest request : requests) {
            if (request == null || request.getCanvasId() == null) {
                continue;
            }
            profiles.computeIfAbsent(request.getCanvasId(), ignored -> new CanvasDispatchProfile())
                    .register(priorityOf(request.getTriggerType()));
        }
        return profiles;
    }

    private TriggerPriorityConfig.Priority priorityOf(String triggerType) {
        return priorityConfig != null ? priorityConfig.of(triggerType) : TriggerPriorityConfig.Priority.NORMAL;
    }

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

    private int normalizePerCanvasLimit(int value) {
        return value <= 0 ? Integer.MAX_VALUE : value;
    }

    private void recordDispatched(Long canvasId) {
        if (metrics != null) {
            metrics.recordExecutionRequestDispatched(normalizeCanvasId(canvasId));
        }
    }

    private void recordSkipped(Long canvasId, String reason) {
        if (metrics != null) {
            metrics.recordExecutionRequestSkipped(normalizeCanvasId(canvasId), reason);
        }
    }

    private void recordDispatchFailure(Long canvasId) {
        if (metrics != null) {
            metrics.recordExecutionRequestDispatchFailure(normalizeCanvasId(canvasId));
        }
    }

    private String normalizeCanvasId(Long canvasId) {
        return canvasId != null ? String.valueOf(canvasId) : "UNKNOWN";
    }

    private static final class CanvasDispatchProfile {
        private int totalRequests;
        private TriggerPriorityConfig.Priority highestPriority = TriggerPriorityConfig.Priority.LOW;

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
