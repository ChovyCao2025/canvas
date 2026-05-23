package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.delivery.MessageSendRecord;
import org.chovy.canvas.domain.delivery.MessageSendRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class ReachDeliveryService {

    private final MessageSendRecordMapper recordMapper;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            @Value("${canvas.integration.reach-platform-url}") String reachPlatformUrl
    ) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().baseUrl(reachPlatformUrl).build();
    }

    public Mono<DeliveryResult> send(DeliveryRequest request) {
        return Mono.fromCallable(() -> prepareRecord(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prepared -> {
                    if (prepared.duplicate()) {
                        boolean sent = !MessageSendRecord.STATUS_FAILED.equals(prepared.record().getStatus());
                        return Mono.just(new DeliveryResult(
                                sent,
                                true,
                                prepared.record().getId(),
                                prepared.record().getExternalMessageId(),
                                prepared.record().getErrorMessage()
                        ));
                    }
                    return callReachPlatform(request)
                            .flatMap(response -> markSent(prepared.record(), response))
                            .onErrorResume(e -> markFailed(prepared.record(), e));
                });
    }

    private PreparedRecord prepareRecord(DeliveryRequest request) {
        MessageSendRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<MessageSendRecord>()
                .eq(MessageSendRecord::getIdempotencyKey, request.idempotencyKey())
                .last("LIMIT 1"));
        if (existing != null) {
            return new PreparedRecord(existing, true);
        }

        MessageSendRecord record = new MessageSendRecord();
        record.setExecutionId(request.executionId());
        record.setCanvasId(request.canvasId());
        record.setUserId(request.userId());
        record.setNodeId(request.nodeId());
        record.setChannel(request.channel());
        record.setTemplateId(request.templateId());
        record.setIdempotencyKey(request.idempotencyKey());
        record.setRequestPayload(toJson(request.payload()));
        record.setStatus(MessageSendRecord.STATUS_PENDING);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(record.getCreatedAt());
        recordMapper.insert(record);
        return new PreparedRecord(record, false);
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> callReachPlatform(DeliveryRequest request) {
        return webClient.post()
                .uri("/send")
                .bodyValue(request.payload())
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map);
    }

    private Mono<DeliveryResult> markSent(MessageSendRecord record, Map<String, Object> response) {
        return Mono.fromCallable(() -> {
                    record.setStatus(MessageSendRecord.STATUS_SENT);
                    Object messageId = response.getOrDefault("messageId", response.get("id"));
                    if (messageId != null) {
                        record.setExternalMessageId(messageId.toString());
                    }
                    record.setUpdatedAt(LocalDateTime.now());
                    recordMapper.updateById(record);
                    return new DeliveryResult(true, false, record.getId(), record.getExternalMessageId(), null);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<DeliveryResult> markFailed(MessageSendRecord record, Throwable error) {
        return Mono.fromCallable(() -> {
                    String message = error.getMessage() == null ? "delivery failed" : error.getMessage();
                    record.setStatus(MessageSendRecord.STATUS_FAILED);
                    record.setErrorMessage(message.substring(0, Math.min(500, message.length())));
                    record.setUpdatedAt(LocalDateTime.now());
                    recordMapper.updateById(record);
                    log.warn("[DELIVERY] 触达失败 recordId={} channel={} reason={}",
                            record.getId(), record.getChannel(), record.getErrorMessage());
                    return new DeliveryResult(false, false, record.getId(), null, record.getErrorMessage());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public DeliveryRequest request(
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String templateId,
            Map<String, Object> content,
            Map<String, Object> variables,
            String idempotencyKey
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", channel);
        payload.put("templateId", templateId);
        payload.put("userId", userId);
        payload.put("content", content == null ? Map.of() : content);
        payload.put("variables", variables == null ? Map.of() : variables);
        payload.put("idempotencyKey", idempotencyKey);
        return new DeliveryRequest(executionId, canvasId, userId, nodeId, channel, templateId, payload, idempotencyKey);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("触达请求序列化失败", e);
        }
    }

    private record PreparedRecord(MessageSendRecord record, boolean duplicate) {
    }

    public record DeliveryRequest(
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String templateId,
            Map<String, Object> payload,
            String idempotencyKey
    ) {
    }

    public record DeliveryResult(
            boolean sent,
            boolean duplicate,
            Long recordId,
            String externalMessageId,
            String errorMessage
    ) {
    }
}
