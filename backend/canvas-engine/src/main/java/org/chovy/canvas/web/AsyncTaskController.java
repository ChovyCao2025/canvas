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

    /** 异步任务服务，用于查询任务状态和订阅人。 */
    private final AsyncTaskService taskService;

    /**
     * 处理 get 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param taskId taskId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{taskId}")
    public Mono<R<AsyncTaskDTO>> get(@PathVariable String taskId) {
        return currentUser().flatMap(user ->
                Mono.fromCallable(() -> {
                    // 任务查询走阻塞持久化层，外层切线程池后再做权限过滤。
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
                                // 非管理员只查本人创建或订阅的任务，服务层继续执行数据范围约束。
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

    /**
     * 执行 current User 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<CurrentUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(claims -> new CurrentUser(
                        // 安全上下文缺失字段时降级为普通操作员，避免误给管理员权限。
                        defaultIfBlank(claims.get("username", String.class), "system"),
                        defaultIfBlank(claims.get("role", String.class), "OPERATOR")))
                .defaultIfEmpty(new CurrentUser("system", "OPERATOR"));
    }

    /**
     * 构建、解析或转换 parse Csv 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 查询、转换或计算得到的结果集合
     */
    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    /**
     * 执行 clamp 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param min min 方法执行所需的业务参数
     * @param max max 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 执行 default If Blank 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 判断 can View 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param task task 方法执行所需的业务参数
     * @param user user 用户或客户相关标识/数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean canView(AsyncTaskDO task, CurrentUser user) {
        if ("ADMIN".equals(user.role())) {
            return true;
        }
        if (user.username().equals(task.getCreatedBy())) {
            return true;
        }
        // 订阅者可以查看异步任务进度，但不能因此获得管理员视图。
        return taskService.subscribers(task.getTaskId()).contains(user.username());
    }

    private record CurrentUser(
            /** 用户名。 */
            String username,
            /** 角色编码。 */
            String role
    ) {
    }
}
