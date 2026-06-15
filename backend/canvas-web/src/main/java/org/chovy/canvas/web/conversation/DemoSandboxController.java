package org.chovy.canvas.web.conversation;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.conversation.api.DemoSandboxFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/demo-sandboxes")
public class DemoSandboxController {

    private static final String DEFAULT_ACTOR = "system";

    private final DemoSandboxFacade facade;

    public DemoSandboxController(DemoSandboxFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<DemoSandboxFacade.SandboxView>> install(
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) DemoSandboxFacade.InstallCommand command) {
        return envelope(() -> facade.install(command, actorOrDefault(actor)));
    }

    @PostMapping("/{tenantId}/reset")
    public Mono<CompatibilityEnvelope<DemoSandboxFacade.ResetResult>> reset(
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long tenantId) {
        return envelope(() -> facade.reset(tenantId, actorOrDefault(actor)));
    }

    @GetMapping("/expired")
    public Mono<CompatibilityEnvelope<List<DemoSandboxFacade.SandboxView>>> expired() {
        return envelope(facade::expired);
    }

    @PostMapping("/{tenantId}/conversation-replies")
    public Mono<CompatibilityEnvelope<DemoSandboxFacade.ConversationReplyResult>> reply(
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long tenantId,
            @RequestBody(required = false) DemoSandboxFacade.ConversationReplyCommand command) {
        return envelope(() -> facade.reply(tenantId, command, actorOrDefault(actor)));
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
    }
}
