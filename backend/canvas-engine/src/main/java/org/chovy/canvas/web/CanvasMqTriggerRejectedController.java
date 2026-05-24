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

@Slf4j
@RestController
@RequestMapping("/canvas/mq-trigger-rejected")
@RequiredArgsConstructor
public class CanvasMqTriggerRejectedController {

    private final CanvasMqTriggerRejectedMapper mapper;
    private final TriggerRouteService routeService;
    private final CanvasExecutionRequestService requestService;
    private final CanvasDisruptorService disruptorService;
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

    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replay(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            CanvasMqTriggerRejectedDO rejected = mapper.selectById(id);
            if (rejected == null) {
                throw new IllegalArgumentException("rejected 消息不存在: " + id);
            }
            MqTriggerMessage message = parseMessage(rejected);
            validateMessage(message);

            List<String> requestIds = new ArrayList<>();
            List<String> dispatchFailed = new ArrayList<>();
            routeService.getCanvasByMqTopic(rejected.getTag()).stream()
                    .map(this::parseCanvasId)
                    .flatMap(java.util.Optional::stream)
                    .sorted()
                    .forEach(canvasId -> {
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

    private MqTriggerMessage parseMessage(CanvasMqTriggerRejectedDO rejected) {
        try {
            return objectMapper.readValue(rejected.getBody(), MqTriggerMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法重放 rejected 消息，消息体不是合法 MQ 触发 JSON", e);
        }
    }

    private void validateMessage(MqTriggerMessage message) {
        if (message.getUserId() == null || message.getUserId().isBlank()
                || message.getMessageCode() == null || message.getMessageCode().isBlank()
                || message.getPayload() == null) {
            throw new IllegalArgumentException("无法重放 rejected 消息，缺少 userId/messageCode/payload");
        }
    }

    private java.util.Optional<Long> parseCanvasId(String raw) {
        try {
            long canvasId = Long.parseLong(raw);
            return canvasId > 0 ? java.util.Optional.of(canvasId) : java.util.Optional.empty();
        } catch (RuntimeException e) {
            log.warn("[MQ_REJECTED] replay skip invalid route canvasId={}", raw);
            return java.util.Optional.empty();
        }
    }

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
