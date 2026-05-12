package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.execution.CanvasExecutionDlq;
import org.chovy.canvas.domain.execution.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DLQ 死信队列管理 API（设计文档 13.3节）。
 * 运营/研发通过此接口查看失败执行并手动触发重放。
 */
@Slf4j
@RestController
@RequestMapping("/canvas/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final CanvasExecutionDlqMapper  dlqMapper;
    private final CanvasExecutionService    executionService;
    private final ObjectMapper              objectMapper;

    /** 查询 DLQ 列表（按画布 or 全部，分页） */
    @GetMapping
    public Mono<R<PageResult<CanvasExecutionDlq>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<CanvasExecutionDlq> q = new LambdaQueryWrapper<CanvasExecutionDlq>()
                    .orderByDesc(CanvasExecutionDlq::getFailedAt);
            if (canvasId != null) q.eq(CanvasExecutionDlq::getCanvasId, canvasId);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<CanvasExecutionDlq> p =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            var result = dlqMapper.selectPage(p, q);
            return PageResult.of(result.getTotal(), result.getRecords());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 重放 DLQ 条目（设计文档 13.3节）。
     * skipSuccessNodes=true：跳过已成功节点，从失败节点重新执行。
     * skipSuccessNodes=false：重新执行整个画布（全量重试）。
     */
    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replay(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean skipSuccessNodes) {
        return Mono.fromCallable(() -> {
            CanvasExecutionDlq dlq = dlqMapper.selectById(id);
            if (dlq == null) throw new IllegalArgumentException("DLQ 记录不存在: " + id);

            // 解析原始 payload
            Map<String, Object> payload = Map.of();
            try {
                payload = objectMapper.readValue(dlq.getTriggerPayload(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("[DLQ] 解析 payload 失败: {}", e.getMessage());
            }

            log.info("[DLQ] 手动重放 dlqId={} canvasId={} userId={} triggerType={} skipSuccessNodes={}",
                    id, dlq.getCanvasId(), dlq.getUserId(), dlq.getTriggerType(), skipSuccessNodes);
            return payload;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(payload -> {
            CanvasExecutionDlq dlq = dlqMapper.selectById(id);
            // 使用原始触发类型重放，不写死 DIRECT_CALL
            String triggerType     = dlq.getTriggerType() != null ? dlq.getTriggerType() : "DLQ_REPLAY";
            String triggerNodeType = dlq.getTriggerNodeType() != null ? dlq.getTriggerNodeType() : "DIRECT_CALL";
            String matchKey        = dlq.getMatchKey();
            return executionService.trigger(
                    dlq.getCanvasId(), dlq.getUserId(), triggerType,
                    triggerNodeType, matchKey,
                    payload,
                    "dlq-replay-" + UUID.randomUUID().toString().substring(0, 8),
                    false);
        })
        .map(result -> R.ok(result));
    }

    /** 删除 DLQ 条目（确认不需要重放时）*/
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> dlqMapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }
}
