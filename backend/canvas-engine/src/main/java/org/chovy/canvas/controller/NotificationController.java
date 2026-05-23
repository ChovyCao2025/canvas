package org.chovy.canvas.controller;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.dto.notification.NotificationDTO;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/canvas/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Mono<R<List<NotificationDTO>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return currentUser().flatMap(userId ->
                Mono.fromCallable(() -> notificationService.list(userId, unreadOnly, page, size)
                                .stream()
                                .map(NotificationDTO::from)
                                .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    @GetMapping("/unread-count")
    public Mono<R<Map<String, Long>>> unreadCount() {
        return currentUser().flatMap(userId ->
                Mono.fromCallable(() -> R.ok(Map.of("count", notificationService.unreadCount(userId))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{notificationId}/read")
    public Mono<R<Void>> markRead(@PathVariable String notificationId) {
        return currentUser().flatMap(userId ->
                Mono.<Void>fromRunnable(() -> notificationService.markRead(userId, notificationId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    @PutMapping("/read-all")
    public Mono<R<Void>> markAllRead() {
        return currentUser().flatMap(userId ->
                Mono.<Void>fromRunnable(() -> notificationService.markAllRead(userId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(claims -> claims.get("username", String.class))
                .defaultIfEmpty("system");
    }
}
