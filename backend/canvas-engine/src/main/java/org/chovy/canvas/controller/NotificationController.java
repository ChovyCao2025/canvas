package org.chovy.canvas.controller;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.notification.NotificationWebSocketTicketService;
import org.chovy.canvas.dto.notification.NotificationDTO;
import org.chovy.canvas.dto.notification.NotificationWebSocketTicketDTO;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final NotificationWebSocketTicketService ticketService;

    @GetMapping
    public Mono<R<List<NotificationDTO>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(1, page);
        int safeSize = clamp(size, 1, 100);
        return currentUser().flatMap(userId ->
                Mono.fromCallable(() -> notificationService.list(userId, unreadOnly, category, archived, safePage, safeSize)
                                .stream()
                                .map(NotificationDTO::from)
                                .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    @GetMapping("/unread-count")
    public Mono<R<Map<String, Long>>> unreadCount() {
        return currentUser().flatMap(userId ->
                Mono.fromCallable(() -> R.ok(Map.of(MapFieldKeys.COUNT, notificationService.unreadCount(userId))))
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

    @PutMapping("/{notificationId}/archive")
    public Mono<R<Void>> archive(@PathVariable String notificationId) {
        return currentUser().flatMap(userId ->
                Mono.<Void>fromRunnable(() -> notificationService.archive(userId, notificationId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    @PostMapping("/ws-ticket")
    public Mono<R<NotificationWebSocketTicketDTO>> createWsTicket() {
        return currentUser().flatMap(userId ->
                Mono.fromCallable(() -> ticketService.createTicket(userId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(ticket -> R.ok(new NotificationWebSocketTicketDTO(
                                ticket,
                                NotificationWebSocketTicketService.TICKET_TTL_SECONDS))));
    }

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(claims -> defaultIfBlank(claims.get("username", String.class), "system"))
                .defaultIfEmpty("system");
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
