package org.chovy.canvas.web;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.AsyncTaskDO;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.task.AsyncTaskDTO;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;

/**
 * 异步任务 HTTP 控制器，根路由为 {@code /canvas/async-tasks}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService taskService;

    @GetMapping("/{taskId}")
    public Mono<R<AsyncTaskDTO>> get(@PathVariable String taskId) {
        return currentUser().flatMap(user ->
                Mono.fromCallable(() -> {
                    AsyncTaskDO task = taskService.getByTaskId(taskId);
                    if (task == null || !canView(task, user)) {
                        throw new IllegalArgumentException("Async task not found: " + taskId);
                    }
                    return AsyncTaskDTO.from(task);
                })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    @GetMapping
    public Mono<R<List<AsyncTaskDTO>>> list(
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizIds,
            @RequestParam(required = false) String statuses,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        List<String> parsedBizIds = parseCsv(bizIds);
        List<String> parsedStatuses = parseCsv(statuses);
        int safePage = Math.max(1, page);
        int safeSize = clamp(size, 1, 200);
        return currentUser().flatMap(user ->
                Mono.fromCallable(() -> taskService.list(
                                taskType,
                                bizType,
                                parsedBizIds,
                                parsedStatuses,
                                user.username(),
                                "ADMIN".equals(user.role()),
                                safePage,
                                safeSize)
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
                        defaultIfBlank(claims.get("username", String.class), "system"),
                        defaultIfBlank(claims.get("role", String.class), "OPERATOR")))
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean canView(AsyncTaskDO task, CurrentUser user) {
        if ("ADMIN".equals(user.role())) {
            return true;
        }
        if (user.username().equals(task.getCreatedBy())) {
            return true;
        }
        return taskService.subscribers(task.getTaskId()).contains(user.username());
    }

    private record CurrentUser(String username, String role) {
    }
}
