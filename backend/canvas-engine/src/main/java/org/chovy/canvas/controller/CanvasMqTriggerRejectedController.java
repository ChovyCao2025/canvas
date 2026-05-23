package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasMqTriggerRejected;
import org.chovy.canvas.domain.execution.CanvasMqTriggerRejectedMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.infra.mq.MqTriggerMessage;
import org.chovy.canvas.infra.redis.TriggerRouteService;
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
    public Mono<R<PageResult<CanvasMqTriggerRejected>>> list(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<CanvasMqTriggerRejected> wrapper = new LambdaQueryWrapper<CanvasMqTriggerRejected>()
                    .eq(tag != null && !tag.isBlank(), CanvasMqTriggerRejected::getTag, tag)
                    .eq(reason != null && !reason.isBlank(), CanvasMqTriggerRejected::getReason, reason)
                    .orderByDesc(CanvasMqTriggerRejected::getCreatedAt);
            Page<CanvasMqTriggerRejected> result = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<R<CanvasMqTriggerRejected>> detail(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            CanvasMqTriggerRejected rejected = mapper.selectById(id);
            if (rejected == null) {
                throw new IllegalArgumentException("rejected 消息不存在: " + id);
            }
            return R.ok(rejected);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replay(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            CanvasMqTriggerRejected rejected = mapper.selectById(id);
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
                    "count", requestIds.size(),
                    "requestIds", requestIds,
                    "dispatchFailureCount", dispatchFailed.size(),
                    "dispatchFailedRequestIds", dispatchFailed
            ));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private MqTriggerMessage parseMessage(CanvasMqTriggerRejected rejected) {
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
