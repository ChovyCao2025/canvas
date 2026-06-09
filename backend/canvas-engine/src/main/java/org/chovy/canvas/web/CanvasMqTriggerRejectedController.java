package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasMqTriggerRejectedDO;
import org.chovy.canvas.dal.mapper.CanvasMqTriggerRejectedMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.infrastructure.mq.MqTriggerMessage;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MQ 触发拒绝记录 HTTP 控制器，根路由为 {@code /canvas/mq-trigger-rejected}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@Slf4j
@RestController
@RequestMapping("/canvas/mq-trigger-rejected")
@RequiredArgsConstructor
public class CanvasMqTriggerRejectedController {

    /** 拒绝消息 Mapper，用于查询和更新拒绝记录。 */
    private final CanvasMqTriggerRejectedMapper mapper;
    /** 触发路由服务，用于解析 MQ 触发路由。 */
    private final TriggerRouteService routeService;
    /** 执行请求服务，用于创建重放请求。 */
    private final CanvasExecutionRequestService requestService;
    /** Disruptor 投递服务，用于重放触发消息。 */
    private final CanvasDisruptorService disruptorService;
    /** JSON 转换器，用于解析拒绝消息 payload。 */
    private final ObjectMapper objectMapper;
    /**
     * 查询画布 MQ 触发拒绝记录列表接口，对应 GET 请求。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tag 请求参数，可选。
     * @param reason 请求参数，可选。
     * @param page 请求参数，默认值为 1。
     * @param size 请求参数，默认值为 20。
     * @return 异步返回统一响应，包含分页结果。
     */
    @GetMapping
    public Mono<R<PageResult<CanvasMqTriggerRejectedDO>>> list(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<CanvasMqTriggerRejectedDO> wrapper = new LambdaQueryWrapper<CanvasMqTriggerRejectedDO>()
                    .eq(tag != null && !tag.isBlank(), CanvasMqTriggerRejectedDO::getTag, tag)
                    .eq(reason != null && !reason.isBlank(), CanvasMqTriggerRejectedDO::getReason, reason)
                    .orderByDesc(CanvasMqTriggerRejectedDO::getCreatedAt);
            Page<CanvasMqTriggerRejectedDO> result = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 detail 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{id}")
    public Mono<R<CanvasMqTriggerRejectedDO>> detail(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            CanvasMqTriggerRejectedDO rejected = mapper.selectById(id);
            if (rejected == null) {
                throw new IllegalArgumentException("rejected 消息不存在: " + id);
            }
            return R.ok(rejected);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 replay 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replay(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            CanvasMqTriggerRejectedDO rejected = mapper.selectById(id);
            if (rejected == null) {
                throw new IllegalArgumentException("rejected 消息不存在: " + id);
            }
            // 先恢复原始 MQ 触发消息，再基于当前路由表重新生成执行请求。
            MqTriggerMessage message = parseMessage(rejected);
            validateMessage(message);

            List<String> requestIds = new ArrayList<>();
            List<String> dispatchFailed = new ArrayList<>();
            routeService.getCanvasByMqTopic(rejected.getTag()).stream()
                    .map(this::parseCanvasId)
                    .flatMap(java.util.Optional::stream)
                    .sorted()
                    .forEach(canvasId -> {
                        // 重放只入执行请求队列，不直接执行节点，保持与正常 MQ 入口相同边界。
                        String requestId = requestService.enqueue(
                                canvasId,
                                message.getUserId(),
                                TriggerType.MQ,
                                NodeType.MQ_TRIGGER,
                                rejected.getTag(),
                                message.getPayload(),
                                rejected.getMsgId()
                        );
                        requestIds.add(requestId);
                        if (!publishBestEffort(requestId)) {
                            // 即时投递失败会返回给调用方，后台补偿仍可根据队列表继续处理。
                            dispatchFailed.add(requestId);
                        }
                    });
            return R.ok(Map.<String, Object>of(
                    MapFieldKeys.COUNT, requestIds.size(),
                    MapFieldKeys.REQUEST_IDS, requestIds,
                    MapFieldKeys.DISPATCH_FAILURE_COUNT, dispatchFailed.size(),
                    MapFieldKeys.DISPATCH_FAILED_REQUEST_IDS, dispatchFailed
            ));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从 rejected 记录中恢复原始 MQ 触发消息。
     *
     * <p>只有原始消息体仍可解析时才允许重放，避免制造新的脏执行请求。
     *
     * @param rejected 待重放的拒绝记录
     * @return 反序列化后的 MQ 触发消息
     */
    private MqTriggerMessage parseMessage(CanvasMqTriggerRejectedDO rejected) {
        try {
            // rejected.body 存的是消费失败时的原始消息体，必须可反序列化后才允许重放。
            return objectMapper.readValue(rejected.getBody(), MqTriggerMessage.class);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("无法重放 rejected 消息，消息体不是合法 MQ 触发 JSON", e);
        }
    }

    /**
     * 校验 rejected 消息重放所需的最小字段。
     *
     * <p>重放入口复用 MQ 消费入口的字段约束，缺字段时立即拒绝。
     *
     * @param message 从 rejected body 恢复出的消息
     */
    private void validateMessage(MqTriggerMessage message) {
        if (message.getUserId() == null || message.getUserId().isBlank()
                || message.getMessageCode() == null || message.getMessageCode().isBlank()
                || message.getPayload() == null) {
            throw new IllegalArgumentException("无法重放 rejected 消息，缺少 userId/messageCode/payload");
        }
    }

    /**
     * 解析当前路由表中的画布 ID。
     *
     * <p>重放按最新路由表执行，路由脏数据只跳过并记录日志。
     *
     * @param raw 路由表保存的画布 ID 字符串
     * @return 合法画布 ID；非法或非正数时为空
     */
    private java.util.Optional<Long> parseCanvasId(String raw) {
        try {
            long canvasId = Long.parseLong(raw);
            // 路由表里可能存在脏数据，非正数 ID 直接跳过。
            return canvasId > 0 ? java.util.Optional.of(canvasId) : java.util.Optional.empty();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            log.warn("[MQ_REJECTED] replay skip invalid route canvasId={}", raw);
            return java.util.Optional.empty();
        }
    }

    /**
     * 尝试立即投递重放请求到 Disruptor。
     *
     * <p>投递失败不回滚已入库的执行请求，后台补偿仍可继续扫描处理。
     *
     * @param requestId 已创建的执行请求 ID
     * @return {@code true} 表示即时投递成功
     */
    private boolean publishBestEffort(String requestId) {
        try {
            disruptorService.publishRequest(requestId);
            return true;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            log.warn("[MQ_REJECTED] replay immediate dispatch failed requestId={} reason={}",
                    requestId, e.getMessage());
            return false;
        }
    }
}
