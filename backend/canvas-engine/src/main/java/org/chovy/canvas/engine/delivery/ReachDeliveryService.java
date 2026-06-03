package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reach Delivery 触达投递组件。
 *
 * <p>封装营销消息发送后的记录写入和投递状态维护，供短信、邮件、Push 等节点复用。
 * <p>外部渠道差异应在调用方或适配层处理，本类聚焦统一的投递流水语义。
 */
@Slf4j
@Service
public class ReachDeliveryService {

    /** 消息发送记录 Mapper，用于落库触达请求、状态和外部消息 ID。 */
    private final MessageSendRecordMapper recordMapper;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** WebClient 客户端。 */
    private final WebClient webClient;

    /** 初始化触达投递依赖，并按配置创建外部触达平台客户端。 */
    public ReachDeliveryService(
            MessageSendRecordMapper recordMapper,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${canvas.integration.reach-platform-url}") String reachPlatformUrl
    ) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.clone().baseUrl(reachPlatformUrl).build();
    }

    /** 执行触达投递并写入发送记录。 */
    public Mono<DeliveryResult> send(DeliveryRequest request) {
        return Mono.fromCallable(() -> prepareRecord(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prepared -> {
                    if (prepared.duplicate()) {
                        // 命中幂等键时直接返回已有流水状态，避免重复调用外部渠道造成二次触达。
                        boolean sent = !MessageSendRecordDO.STATUS_FAILED.equals(prepared.record().getStatus());
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

    /** 准备发送记录；命中幂等键时返回已有记录而不重复投递。 */
    private PreparedRecord prepareRecord(DeliveryRequest request) {
        MessageSendRecordDO existing = recordMapper.selectOne(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getIdempotencyKey, request.idempotencyKey())
                .last("LIMIT 1"));
        if (existing != null) {
            return new PreparedRecord(existing, true);
        }

        // 先落 PENDING 流水再调用外部平台，失败时仍能回写错误并保留审计轨迹。
        MessageSendRecordDO record = new MessageSendRecordDO();
        record.setExecutionId(request.executionId());
        record.setCanvasId(request.canvasId());
        record.setUserId(request.userId());
        record.setNodeId(request.nodeId());
        record.setChannel(request.channel());
        record.setTemplateId(request.templateId());
        record.setIdempotencyKey(request.idempotencyKey());
        record.setRequestPayload(toJson(request.payload()));
        record.setStatus(MessageSendRecordDO.STATUS_PENDING);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(record.getCreatedAt());
        recordMapper.insert(record);
        return new PreparedRecord(record, false);
    }

    /** 调用外部触达平台发送消息并返回渠道响应。 */
    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> callReachPlatform(DeliveryRequest request) {
        return webClient.post()
                .uri("/send")
                // 渠道、模板、变量和幂等键统一放入 payload，具体渠道差异由触达平台适配。
                .bodyValue(request.payload())
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map);
    }

    /** 将发送记录标记为成功并保存外部渠道消息 ID。 */
    private Mono<DeliveryResult> markSent(MessageSendRecordDO record, Map<String, Object> response) {
        return Mono.fromCallable(() -> {
                    record.setStatus(MessageSendRecordDO.STATUS_SENT);
                    Object messageId = response.getOrDefault(MapFieldKeys.MESSAGE_ID, response.get(MapFieldKeys.ID));
                    if (messageId != null) {
                        // 记录外部消息 ID，便于后续回执、排障和人工核对渠道侧状态。
                        record.setExternalMessageId(messageId.toString());
                    }
                    record.setUpdatedAt(LocalDateTime.now());
                    recordMapper.updateById(record);
                    return new DeliveryResult(true, false, record.getId(), record.getExternalMessageId(), null);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 将发送记录标记为失败并保存截断后的错误信息。 */
    private Mono<DeliveryResult> markFailed(MessageSendRecordDO record, Throwable error) {
        return Mono.fromCallable(() -> {
                    String message = error.getMessage() == null ? "delivery failed" : error.getMessage();
                    record.setStatus(MessageSendRecordDO.STATUS_FAILED);
                    record.setErrorMessage(message.substring(0, Math.min(500, message.length())));
                    record.setUpdatedAt(LocalDateTime.now());
                    recordMapper.updateById(record);
                    log.warn("[DELIVERY] 触达失败 recordId={} channel={} reason={}",
                            record.getId(), record.getChannel(), record.getErrorMessage());
                    return new DeliveryResult(false, false, record.getId(), null, record.getErrorMessage());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 构造标准化触达投递请求。 */
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
        // LinkedHashMap 让序列化后的请求字段顺序稳定，便于日志比对和问题排查。
        payload.put(MapFieldKeys.CHANNEL, channel);
        payload.put(MapFieldKeys.TEMPLATE_ID, templateId);
        payload.put(MapFieldKeys.USER_ID, userId);
        payload.put(MapFieldKeys.CONTENT, content == null ? Map.of() : content);
        payload.put(MapFieldKeys.VARIABLES, variables == null ? Map.of() : variables);
        payload.put(MapFieldKeys.IDEMPOTENCY_KEY, idempotencyKey);
        return new DeliveryRequest(executionId, canvasId, userId, nodeId, channel, templateId, payload, idempotencyKey);
    }

    /** 将触达请求载荷序列化为发送记录中的 JSON 文本。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("触达请求序列化失败", e);
        }
    }

    /** 发送前记录准备结果，标识本次是否命中幂等重复请求。 */
    private record PreparedRecord(
            /** 已落库或已命中的发送记录。 */
            MessageSendRecordDO record,
            /** 是否命中幂等重复请求。 */
            boolean duplicate
    ) {
    }

    /** 标准化触达投递请求，封装节点执行产生的渠道、模板、变量和幂等键。 */
    public record DeliveryRequest(
            /* 画布执行实例 ID。 */
            String executionId,
            /* 画布 ID。 */
            Long canvasId,
            /* 接收触达的用户 ID。 */
            String userId,
            /* 发起触达的节点 ID。 */
            String nodeId,
            /* 触达渠道。 */
            String channel,
            /* 渠道模板 ID。 */
            String templateId,
            /* 发送给触达平台的标准化载荷。 */
            Map<String, Object> payload,
            /* 幂等键，用于避免重复触达。 */
            String idempotencyKey
    ) {
    }

    /** 触达投递结果，返回是否发送、是否命中幂等、记录 ID 和外部渠道回执。 */
    public record DeliveryResult(
            /* 是否已成功提交触达。 */
            boolean sent,
            /* 是否命中幂等重复请求。 */
            boolean duplicate,
            /* 对应的发送记录 ID。 */
            Long recordId,
            /* 外部渠道返回的消息 ID。 */
            String externalMessageId,
            /* 失败原因，成功时为空。 */
            String errorMessage
    ) {
    }
}
