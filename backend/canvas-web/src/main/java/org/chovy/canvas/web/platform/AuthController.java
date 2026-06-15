package org.chovy.canvas.web.platform;

import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.AuthFacade;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthFacade facade;

    public AuthController(AuthFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/login")
    public Mono<CompatibilityEnvelope<AuthFacade.LoginView>> login(
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.login(new AuthFacade.LoginCommand(text(payload, "username"),
                text(payload, "password"))));
    }

    @PostMapping("/logout")
    public Mono<CompatibilityEnvelope<AuthFacade.LogoutView>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return envelope(() -> facade.logout(authorizationHeader));
    }

    @GetMapping("/me")
    public Mono<CompatibilityEnvelope<AuthFacade.LoginView>> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return envelope(() -> facade.me(authorizationHeader));
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

    private static String text(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
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
