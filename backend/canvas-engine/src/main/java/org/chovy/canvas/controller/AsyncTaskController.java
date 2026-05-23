package org.chovy.canvas.controller;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.task.AsyncTaskDTO;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/canvas/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService taskService;

    @GetMapping
    public Mono<R<List<AsyncTaskDTO>>> list(
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizIds,
            @RequestParam(required = false) String statuses
    ) {
        List<String> parsedBizIds = parseCsv(bizIds);
        List<String> parsedStatuses = parseCsv(statuses);
        return currentUser().flatMap(user ->
                Mono.fromCallable(() -> taskService.list(
                                taskType,
                                bizType,
                                parsedBizIds,
                                parsedStatuses,
                                user.username(),
                                "ADMIN".equals(user.role()))
                        .stream()
                        .map(AsyncTaskDTO::from)
                        .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    private Mono<CurrentUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(claims -> new CurrentUser(
                        claims.get("username", String.class),
                        claims.get("role", String.class)))
                .defaultIfEmpty(new CurrentUser("system", "OPERATOR"));
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private record CurrentUser(String username, String role) {
    }
}
