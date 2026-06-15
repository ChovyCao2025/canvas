package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.execution.api.MessageSendRecordFacade;
import org.chovy.canvas.execution.api.MessageSendRecordFacade.MessageSendRecordPageView;
import org.chovy.canvas.execution.api.MessageSendRecordFacade.MessageSendRecordQuery;
import org.chovy.canvas.execution.domain.MessageSendRecordCatalog.MessageSendRecord;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/message-send-records")
public class MessageSendRecordController {

    private final MessageSendRecordFacade facade;

    public MessageSendRecordController(MessageSendRecordFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<PageResult<MessageSendRecordResponse>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return envelope(() -> {
            MessageSendRecordPageView result = facade.search(new MessageSendRecordQuery(
                    canvasId, executionId, userId, channel, status, startAt, endAt, page, size));
            return CompatibilityEnvelope.ok(new PageResult<>(
                    result.total(),
                    result.list().stream().map(MessageSendRecordResponse::from).toList()));
        });
    }

    @GetMapping("/{id}")
    public Mono<CompatibilityEnvelope<MessageSendRecordResponse>> detail(@PathVariable Long id) {
        return envelope(() -> facade.findById(id)
                .map(MessageSendRecordResponse::from)
                .map(CompatibilityEnvelope::ok)
                .orElseGet(() -> CompatibilityEnvelope.fail("发送记录不存在: " + id)));
    }

    private static <T> Mono<T> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(supplier::get)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String message) {
            return new CompatibilityEnvelope<>(-1, message, null, null, null);
        }
    }

    private record PageResult<T>(long total, List<T> list) {
    }

    private record MessageSendRecordResponse(
            Long id,
            Long tenantId,
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String templateId,
            String idempotencyKey,
            String requestPayload,
            String status,
            String externalMessageId,
            String errorMessage,
            String createdAt,
            String updatedAt) {
        private static MessageSendRecordResponse from(MessageSendRecord record) {
            return new MessageSendRecordResponse(
                    record.id(),
                    record.tenantId(),
                    record.executionId(),
                    record.canvasId(),
                    record.userId(),
                    record.nodeId(),
                    record.channel(),
                    record.templateId(),
                    record.idempotencyKey(),
                    record.requestPayload(),
                    record.status(),
                    record.externalMessageId(),
                    record.errorMessage(),
                    format(record.createdAt()),
                    format(record.updatedAt()));
        }

        private static String format(LocalDateTime value) {
            return value == null ? null : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        }
    }
}
