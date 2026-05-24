package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;

/**
 * DLQ 死信队列管理 API（设计文档 13.3节）。
 * 运营/研发通过此接口查看失败执行并手动触发重放。
 *
 * 约束：
 * - 重放请求只负责“再次触发”，不自动删除原死信；
 * - 是否删除死信由调用方在确认结果后显式操作。
 */
@Slf4j
@RestController
@RequestMapping("/canvas/dlq")
@RequiredArgsConstructor
public class DlqController {

    /** DLQ 明细表访问层。 */
    private final CanvasExecutionDlqMapper dlqMapper;

    /** 执行引擎入口，用于把死信重新送入执行链路。 */
    private final CanvasExecutionService executionService;

    /** payload 反序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 查询 DLQ 列表（分页）
     *
     * @param canvasId 画布 ID（可选）
     * @param page     页码
     * @param size     每页大小
     * @return DLQ 条目分页列表
     */
    @GetMapping
    public Mono<R<PageResult<CanvasExecutionDlqDO>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<CanvasExecutionDlqDO> q = new LambdaQueryWrapper<CanvasExecutionDlqDO>()
                    .orderByDesc(CanvasExecutionDlqDO::getFailedAt);
            if (canvasId != null) q.eq(CanvasExecutionDlqDO::getCanvasId, canvasId);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<CanvasExecutionDlqDO> p =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            var result = dlqMapper.selectPage(p, q);
            return PageResult.of(result.getTotal(), result.getRecords());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 重放 DLQ 条目
     *
     * @param id               DLQ 记录 ID
     * @param skipSuccessNodes 是否跳过已成功的节点
     * @return 重放执行结果
     */
    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replay(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean skipSuccessNodes) {
        return Mono.fromCallable(() -> {
                    CanvasExecutionDlqDO dlq = dlqMapper.selectById(id);
                    if (dlq == null) throw new IllegalArgumentException("DLQ 记录不存在: " + id);

                    // 解析原始 payload
                    Map<String, Object> payload = Map.of();
                    try {
                        payload = objectMapper.readValue(dlq.getTriggerPayload(), new TypeReference<>() {
                        });
                    } catch (Exception e) {
                        log.warn("[DLQ] 解析 payload 失败: {}", e.getMessage());
                    }

                    log.info("[DLQ] 手动重放 dlqId={} canvasId={} userId={} triggerType={} skipSuccessNodes={}",
                            id, dlq.getCanvasId(), dlq.getUserId(), dlq.getTriggerType(), skipSuccessNodes);
                    return payload;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(payload -> {
                    // 再次读取记录，确保重放阶段使用的是最新死信元信息
                    CanvasExecutionDlqDO dlq = dlqMapper.selectById(id);
                    // 使用原始触发类型重放，不写死 DIRECT_CALL
                    String triggerType = dlq.getTriggerType() != null ? dlq.getTriggerType() : TriggerType.DLQ_REPLAY;
                    String triggerNodeType = dlq.getTriggerNodeType() != null ? dlq.getTriggerNodeType() : NodeType.DIRECT_CALL;
                    String matchKey = dlq.getMatchKey();
                    return executionService.trigger(
                            dlq.getCanvasId(), dlq.getUserId(), triggerType,
                            triggerNodeType, matchKey,
                            payload,
                            "dlq-replay-" + UUID.randomUUID().toString().substring(0, 8),
                            false);
                })
                .map(R::ok);
    }

    /**
     * 删除 DLQ 条目
     *
     * @param id DLQ 记录 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> dlqMapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

}
