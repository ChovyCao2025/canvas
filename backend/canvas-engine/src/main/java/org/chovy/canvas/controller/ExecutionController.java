package org.chovy.canvas.controller;

import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class ExecutionController {

    private final CanvasExecutionService  executionService;
    private final CanvasDisruptorService  disruptorService;  // 12.8节 Disruptor 分发

    /** 业务直调：同步等待结果（绕过 Disruptor，直接执行） */
    @PostMapping("/execute/direct/{canvasId}")
    public Mono<R<Map<String, Object>>> directCall(
            @PathVariable Long canvasId,
            @RequestBody DirectCallReq req) {
        return executionService.trigger(
                canvasId, req.getUserId(), "DIRECT_CALL",
                "DIRECT_CALL", null,
                req.getInputParams(), UUID.randomUUID().toString(), false)
                .map(R::ok);
    }

    /**
     * 端内行为触发：异步，经过 Disruptor Ring Buffer 削峰（12.8节）。
     * 立即返回 200，Disruptor 消费者异步执行。
     */
    @PostMapping("/trigger/behavior")
    public Mono<R<Void>> behaviorTrigger(@RequestBody BehaviorTriggerReq req) {
        disruptorService.publish(
                req.getCanvasId(), req.getUserId(), "BEHAVIOR",
                "BEHAVIOR_IN_APP", req.getEventCode(),
                req.getBehaviorData(), req.getEventId());
        return Mono.just(R.ok());
    }

    /** 干运行：不走 Disruptor，直接同步执行（不产生真实副作用） */
    @PostMapping("/execute/dry-run/{canvasId}")
    public Mono<R<Map<String, Object>>> dryRun(
            @PathVariable Long canvasId,
            @RequestBody DirectCallReq req) {
        return executionService.trigger(
                canvasId, req.getUserId(), "DRY_RUN",
                "DIRECT_CALL", null,
                req.getInputParams(), UUID.randomUUID().toString(), true)
                .map(R::ok);
    }

    @Data
    static class DirectCallReq {
        private String userId;
        private Map<String, Object> inputParams;
    }

    @Data
    static class BehaviorTriggerReq {
        private Long   canvasId;
        private String userId;
        private String eventCode;
        private String eventId;
        private Map<String, Object> behaviorData;
    }
}
