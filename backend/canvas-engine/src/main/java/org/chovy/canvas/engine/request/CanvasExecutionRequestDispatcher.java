package org.chovy.canvas.engine.request;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CanvasExecutionRequestDispatcher {

    private final CanvasExecutionRequestMapper mapper;
    private final CanvasDisruptorService disruptorService;
    private final int batchSize;
    private final long runningStaleSeconds;
    private final int perCanvasBatchLimit;

    @Autowired
    public CanvasExecutionRequestDispatcher(CanvasExecutionRequestMapper mapper,
                                            CanvasDisruptorService disruptorService,
                                            @Value("${canvas.execution-request.dispatch-batch-size:200}") int batchSize,
                                            @Value("${canvas.execution-request.running-stale-sec:300}") long runningStaleSeconds,
                                            @Value("${canvas.execution-request.per-canvas-batch-limit:0}") int perCanvasBatchLimit) {
        this.mapper = mapper;
        this.disruptorService = disruptorService;
        this.batchSize = batchSize;
        this.runningStaleSeconds = runningStaleSeconds;
        this.perCanvasBatchLimit = normalizePerCanvasLimit(perCanvasBatchLimit);
    }

    public CanvasExecutionRequestDispatcher(CanvasExecutionRequestMapper mapper,
                                            CanvasDisruptorService disruptorService,
                                            int batchSize,
                                            long runningStaleSeconds) {
        this(mapper, disruptorService, batchSize, runningStaleSeconds, 0);
    }

    @Scheduled(fixedDelayString = "${canvas.execution-request.dispatch-fixed-delay-ms:1000}")
    public void dispatchDueRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(runningStaleSeconds);
        List<CanvasExecutionRequest> requests = mapper.selectDueRequests(batchSize, now, staleBefore);
        Map<Long, Integer> canvasCounts = new HashMap<>();
        for (CanvasExecutionRequest request : requests) {
            if (request == null || request.getId() == null) {
                continue;
            }
            if (exceedsPerCanvasLimit(request.getCanvasId(), canvasCounts)) {
                continue;
            }
            try {
                disruptorService.publishRequest(request.getId());
            } catch (RuntimeException e) {
                log.warn("[EXEC_REQUEST] dispatch stopped requestId={} reason={}", request.getId(), e.getMessage());
                return;
            }
        }
    }

    private boolean exceedsPerCanvasLimit(Long canvasId, Map<Long, Integer> canvasCounts) {
        if (perCanvasBatchLimit == Integer.MAX_VALUE || canvasId == null) {
            return false;
        }
        int count = canvasCounts.getOrDefault(canvasId, 0);
        if (count >= perCanvasBatchLimit) {
            return true;
        }
        canvasCounts.put(canvasId, count + 1);
        return false;
    }

    private int normalizePerCanvasLimit(int value) {
        return value <= 0 ? Integer.MAX_VALUE : value;
    }
}
