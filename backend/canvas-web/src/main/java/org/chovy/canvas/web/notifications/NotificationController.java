package org.chovy.canvas.web.notifications;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.NotificationFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/notifications")
public class NotificationController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "system";

    private final NotificationFacade facade;

    public NotificationController(NotificationFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        return envelope(() -> facade.list(tenantIdOrDefault(tenantId), actorOrDefault(actor), unreadOnly, archived,
                category, safePage, safeSize));
    }

    @GetMapping("/unread-count")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> unreadCount(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.unreadCount(tenantIdOrDefault(tenantId), actorOrDefault(actor)));
    }

    @PutMapping("/{notificationId}/read")
    public Mono<CompatibilityEnvelope<Void>> markRead(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String notificationId) {
        return emptyEnvelope(() -> facade.markRead(tenantIdOrDefault(tenantId), actorOrDefault(actor), notificationId));
    }

    @PutMapping("/read-all")
    public Mono<CompatibilityEnvelope<Void>> markAllRead(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return emptyEnvelope(() -> facade.markAllRead(tenantIdOrDefault(tenantId), actorOrDefault(actor)));
    }

    @PutMapping("/{notificationId}/archive")
    public Mono<CompatibilityEnvelope<Void>> archive(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String notificationId) {
        return emptyEnvelope(() -> facade.archive(tenantIdOrDefault(tenantId), actorOrDefault(actor), notificationId));
    }

    @PostMapping("/ws-ticket")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createWsTicket(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.createWsTicket(tenantIdOrDefault(tenantId), actorOrDefault(actor)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> security(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CompatibilityEnvelope.forbidden(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Mono<CompatibilityEnvelope<Void>> emptyEnvelope(Runnable runnable) {
        return Mono.fromRunnable(runnable)
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(CompatibilityEnvelope.ok(null));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }

        private static CompatibilityEnvelope<Object> forbidden(String message) {
            return new CompatibilityEnvelope<>(403, message, "AUTH_003", null, null);
        }
    }
}
