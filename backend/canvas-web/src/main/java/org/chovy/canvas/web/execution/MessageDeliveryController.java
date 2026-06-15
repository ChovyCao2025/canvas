package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.execution.api.MessageDeliveryFacade;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.DeliveryPageView;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.DeliverySearchQuery;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.ReconcileResultView;
import org.chovy.canvas.execution.api.MessageDeliveryFacade.ReplayResultView;
import org.chovy.canvas.execution.domain.MessageDeliveryCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/message-deliveries")
public class MessageDeliveryController {

    private final MessageDeliveryFacade facade;

    public MessageDeliveryController(MessageDeliveryFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<DeliveryPageView>> list(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerMessageId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return envelope(() -> CompatibilityEnvelope.ok(facade.search(new DeliverySearchQuery(
                tenantId, canvasId, executionId, userId, channel, provider, status, providerMessageId, page, size))));
    }

    @GetMapping("/{id}")
    public Mono<CompatibilityEnvelope<MessageDeliveryCatalog.Delivery>> detail(@PathVariable Long id) {
        return envelope(() -> facade.findById(id)
                .map(CompatibilityEnvelope::ok)
                .orElseGet(() -> CompatibilityEnvelope.fail("message delivery not found: " + id)));
    }

    @GetMapping("/{id}/receipts")
    public Mono<CompatibilityEnvelope<List<MessageDeliveryCatalog.Receipt>>> receipts(@PathVariable Long id) {
        return envelope(() -> CompatibilityEnvelope.ok(facade.receipts(id)));
    }

    @PostMapping("/{id}/replay")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> replay(@PathVariable Long id) {
        return envelope(() -> {
            ReplayResultView result = facade.replay(id);
            if (!result.replayed()) {
                return CompatibilityEnvelope.fail("delivery is not replayable: " + id);
            }
            return CompatibilityEnvelope.ok(Map.<String, Object>of("outboxId", id, "status", result.status()));
        });
    }

    @PostMapping("/reconcile")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> reconcile() {
        return envelope(() -> {
            ReconcileResultView result = facade.reconcile();
            return CompatibilityEnvelope.ok(Map.<String, Object>of("requeued", result.requeued()));
        });
    }

    private static <T> Mono<T> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(supplier::get)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String message) {
            return new CompatibilityEnvelope<>(1, message, null, null, null);
        }
    }
}
