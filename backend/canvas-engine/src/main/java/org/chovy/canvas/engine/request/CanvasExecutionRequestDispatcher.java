package org.chovy.canvas.engine.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CanvasExecutionRequestDispatcher {

    private final CanvasExecutionRequestMapper mapper;
    private final CanvasDisruptorService disruptorService;
    private final int batchSize;
    private final long runningStaleSeconds;

    public CanvasExecutionRequestDispatcher(CanvasExecutionRequestMapper mapper,
                                            CanvasDisruptorService disruptorService,
                                            @Value("${canvas.execution-request.dispatch-batch-size:200}") int batchSize,
                                            @Value("${canvas.execution-request.running-stale-sec:300}") long runningStaleSeconds) {
        this.mapper = mapper;
        this.disruptorService = disruptorService;
        this.batchSize = batchSize;
        this.runningStaleSeconds = runningStaleSeconds;
    }

    @Scheduled(fixedDelayString = "${canvas.execution-request.dispatch-fixed-delay-ms:1000}")
    public void dispatchDueRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(runningStaleSeconds);
        List<String> requestIds = mapper.selectDue(batchSize, now, staleBefore);
        for (String requestId : requestIds) {
            try {
                disruptorService.publishRequest(requestId);
            } catch (RuntimeException e) {
                log.warn("[EXEC_REQUEST] dispatch stopped requestId={} reason={}", requestId, e.getMessage());
                return;
            }
        }
    }
}
