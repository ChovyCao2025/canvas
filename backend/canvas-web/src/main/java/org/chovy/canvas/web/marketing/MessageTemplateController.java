package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.MessageTemplateFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/message-templates")
public class MessageTemplateController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_ACTOR = "system";

    private final MessageTemplateFacade facade;

    public MessageTemplateController(MessageTemplateFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<MessageTemplateFacade.TemplateView>>> search(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel) {
        return envelope(() -> facade.search(tenantIdOrDefault(tenantId), keyword, channel));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<MessageTemplateFacade.TemplateView>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody MessageTemplateFacade.TemplateDraft draft) {
        return envelope(() -> facade.create(tenantIdOrDefault(tenantId), actorOrDefault(actor), draft));
    }

    @PostMapping("/{templateCode}/preview")
    public Mono<CompatibilityEnvelope<MessageTemplateFacade.PreviewView>> preview(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String templateCode,
            @RequestBody(required = false) Map<String, Object> context) {
        return envelope(() -> facade.preview(tenantIdOrDefault(tenantId), templateCode, safePayload(context)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
