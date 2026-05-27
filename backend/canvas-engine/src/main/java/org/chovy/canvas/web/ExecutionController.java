package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.UUID;

/**
 * 画布执行控制器：
 * 提供直调执行、行为触发异步投递与 dry-run 调试入口。
 */
@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class ExecutionController {

    /** 执行服务，用于直调和 dry-run 画布执行。 */
    private final CanvasExecutionService executionService;
    // 12.8节 Disruptor 分发
    /** Disruptor 投递服务，用于异步发布行为触发任务。 */
    private final CanvasDisruptorService disruptorService;

    /**
     * 业务直调接口：同步执行并等待结果
     *
     * @param canvasId 画布 ID
     * @param req      直调请求参数（包含用户 ID、输入参数、幂等 Key）
     * @return 执行结果
     */
    @PostMapping("/execute/direct/{canvasId}")
    public Mono<R<Map<String, Object>>> directCall(
            @PathVariable Long canvasId,
            @RequestBody DirectCallReq req) {
        // 设计文档 13.1节：调用方应提供 idempotencyKey，网络超时重试时保持相同值可防重复执行
        // 未提供时生成随机 UUID（不保证幂等，业务方需知晓风险）
        // 压测请求通过 inputParams.perfRunId 贯穿账本，通过 idempotencyKey 控制唯一输入键。
        String dedupKey = (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank())
                ? req.getIdempotencyKey()
                : UUID.randomUUID().toString();
        // 从 Reactor 安全上下文取当前登录用户，避免信任请求体里的 userId。
        return currentUserId().flatMap(userId ->
                executionService.trigger(
                                canvasId, userId, NodeType.DIRECT_CALL,
                                NodeType.DIRECT_CALL, null,
                                req.getInputParams(), dedupKey, false)
                        .map(R::ok));
    }

    /**
     * 端内行为触发：异步，经过 Disruptor Ring Buffer 削峰（12.8节）。
     * 立即返回 200，Disruptor 消费者异步执行。
     *
     * @param req 行为触发请求参数
     * @return 成功响应
     */
    @PostMapping("/trigger/behavior")
    public Mono<R<Void>> behaviorTrigger(@RequestBody BehaviorTriggerReq req) {
        // 控制器只负责投递 Ring Buffer，实际画布执行由 Disruptor 消费线程异步完成。
        disruptorService.publish(
                req.getCanvasId(), req.getUserId(), "BEHAVIOR",
                NodeType.EVENT_TRIGGER, req.getEventCode(),
                req.getBehaviorData(), req.getEventId());
        return Mono.just(R.ok());
    }

    /**
     * 干运行：不走 Disruptor，直接同步执行（不产生真实副作用）
     *
     * @param canvasId 画布 ID
     * @param req      请求参数
     * @return 干运行执行结果
     */
    @PostMapping("/execute/dry-run/{canvasId}")
    public Mono<R<Map<String, Object>>> dryRun(
            @PathVariable Long canvasId,
            @RequestBody DirectCallReq req) {
        // dry-run 使用请求中的 graphJson，不读取线上发布版本，也不产生真实副作用。
        return currentUserId().flatMap(userId ->
                executionService.triggerDryRun(
                                canvasId, userId,
                                req.getInputParams(), req.getGraphJson())
                        .map(R::ok));
    }

    /**
     * 执行 current User Id 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(claims -> {
                    // 优先使用 subject 中的用户 ID；旧 token 缺失 subject 时兼容 username。
                    String subject = claims.getSubject();
                    if (subject != null && !subject.isBlank()) {
                        return subject;
                    }
                    String username = claims.get("username", String.class);
                    return username != null && !username.isBlank() ? username : "system";
                })
                .defaultIfEmpty("system");
    }


    /**
     * 直调/干运行请求体。
     */
    @Data
    static class DirectCallReq {

        /** 触发用户 ID。 */
        private String userId;

        /** 输入参数（注入到执行上下文 triggerPayload）。 */
        private Map<String, Object> inputParams;

        /** 幂等键（建议调用方传入，便于重试去重）。 */
        private String idempotencyKey;

        /** dry-run 时传入当前画布 graphJson，直接使用而不读 DB draft。 */
        private String graphJson;
    }

    /**
     * 行为触发请求体（异步投递 Disruptor）。
     */
    @Data
    static class BehaviorTriggerReq {

        /** 目标画布 ID。 */
        private Long canvasId;

        /** 触发用户 ID。 */
        private String userId;

        /** 行为事件编码。 */
        private String eventCode;

        /** 事件唯一 ID（用于幂等去重）。 */
        private String eventId;

        /** 行为事件载荷。 */
        private Map<String, Object> behaviorData;
    }
}
