package org.chovy.canvas.engine.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount;

/**
 * 画布执行请求 Backlog Metrics 执行请求治理组件。
 *
 * <p>负责画布执行请求的排队、派发、限流、重放或积压度量，削峰高并发触发流量。
 * <p>该层位于触发入口和 DAG 执行之间，核心目标是保护执行引擎稳定性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasExecutionRequestBacklogMetrics {

    /** 需要刷新积压水位的执行请求状态集合。 */
    private static final List<String> STATUSES = List.of(
            CanvasExecutionRequestStatus.PENDING,
            CanvasExecutionRequestStatus.RETRY,
            CanvasExecutionRequestStatus.RUNNING
    );

    /** 执行请求 Mapper，用于读取状态聚合计数。 */
    private final CanvasExecutionRequestMapper mapper;
    /** 画布指标埋点器，用于写入 backlog gauge。 */
    private final CanvasMetrics metrics;

    /**
     * refresh 更新 engine.request 场景的业务状态。
     */
    @Scheduled(fixedDelayString = "${canvas.execution-request.backlog-metrics-delay-ms:10000}")
    public void refresh() {
        try {
            Map<String, Long> counts = new HashMap<>();
            for (CanvasExecutionRequestStatusCount row : mapper.countByStatus()) {
                if (row == null || row.getStatus() == null) {
                    continue;
                }
                // 同一状态可能分散在多条统计行里，这里统一聚合后再写回指标。
                counts.merge(row.getStatus(), row.getCount() != null ? row.getCount() : 0L, Long::sum);
            }
            for (String status : STATUSES) {
                // 只刷新执行请求的核心 backlog 状态，给调度和告警消费。
                metrics.setExecutionRequestBacklog(status, counts.getOrDefault(status, 0L));
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            log.warn("[EXEC_REQUEST] refresh backlog metrics failed: {}", e.getMessage());
        }
    }
}
