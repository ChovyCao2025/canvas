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

    private static final List<String> STATUSES = List.of(
            CanvasExecutionRequestStatus.PENDING,
            CanvasExecutionRequestStatus.RETRY,
            CanvasExecutionRequestStatus.RUNNING
    );

    private final CanvasExecutionRequestMapper mapper;
    private final CanvasMetrics metrics;

    @Scheduled(fixedDelayString = "${canvas.execution-request.backlog-metrics-delay-ms:10000}")
    public void refresh() {
        try {
            Map<String, Long> counts = new HashMap<>();
            for (CanvasExecutionRequestStatusCount row : mapper.countByStatus()) {
                if (row == null || row.getStatus() == null) {
                    continue;
                }
                counts.merge(row.getStatus(), row.getCount() != null ? row.getCount() : 0L, Long::sum);
            }
            for (String status : STATUSES) {
                metrics.setExecutionRequestBacklog(status, counts.getOrDefault(status, 0L));
            }
        } catch (RuntimeException e) {
            log.warn("[EXEC_REQUEST] refresh backlog metrics failed: {}", e.getMessage());
        }
    }
}
