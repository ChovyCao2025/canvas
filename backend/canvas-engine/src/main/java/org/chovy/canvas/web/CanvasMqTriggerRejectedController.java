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
     * 构建、解析或转换 parse Message 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param rejected rejected 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private MqTriggerMessage parseMessage(CanvasMqTriggerRejectedDO rejected) {
        try {
            // rejected.body 存的是消费失败时的原始消息体，必须可反序列化后才允许重放。
            return objectMapper.readValue(rejected.getBody(), MqTriggerMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法重放 rejected 消息，消息体不是合法 MQ 触发 JSON", e);
        }
    }

        /**
     * 校验 validate Message 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param message message 方法执行所需的业务参数
     */
    private void validateMessage(MqTriggerMessage message) {
        if (message.getUserId() == null || message.getUserId().isBlank()
                || message.getMessageCode() == null || message.getMessageCode().isBlank()
                || message.getPayload() == null) {
            throw new IllegalArgumentException("无法重放 rejected 消息，缺少 userId/messageCode/payload");
        }
    }

        /**
     * 构建、解析或转换 parse Canvas Id 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param raw raw 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    private java.util.Optional<Long> parseCanvasId(String raw) {
        try {
            long canvasId = Long.parseLong(raw);
            // 路由表里可能存在脏数据，非正数 ID 直接跳过。
            return canvasId > 0 ? java.util.Optional.of(canvasId) : java.util.Optional.empty();
        } catch (RuntimeException e) {
            log.warn("[MQ_REJECTED] replay skip invalid route canvasId={}", raw);
            return java.util.Optional.empty();
        }
    }

        /**
     * 发布或发送 publish Best Effort 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param requestId requestId 对应的业务主键或标识
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean publishBestEffort(String requestId) {
        try {
            disruptorService.publishRequest(requestId);
            return true;
        } catch (RuntimeException e) {
            log.warn("[MQ_REJECTED] replay immediate dispatch failed requestId={} reason={}",
                    requestId, e.getMessage());
            return false;
        }
    }
}
