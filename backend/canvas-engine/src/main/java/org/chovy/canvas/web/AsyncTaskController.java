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
                        /**
                         * 执行 illegalargumentexception 对应的内部处理流程。
                         *
                         * @param taskId task标识，由调用方提供
                         * @return 返回内部处理结果
                         */
                        throw new IllegalArgumentException("Async task not found: " + taskId);
                    }
                    return AsyncTaskDTO.from(task);
                })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 查询异步任务列表接口，对应 GET 请求。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 taskService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param taskType 请求参数，可选。
     * @param bizType 请求参数，可选。
     * @param bizIds 请求参数，可选。
     * @param statuses 请求参数，可选。
     * @param page 请求参数，默认值为 1。
     * @param size 请求参数，默认值为 100。
     * @return 异步返回统一响应，包含列表结果。
     */
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
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentUser 流程生成的业务结果。
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
     * 解析并校验输入数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> parseCsv(String value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || value.isBlank()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param min min 参数，用于 clamp 流程中的校验、计算或对象转换。
     * @param max max 参数，用于 clamp 流程中的校验、计算或对象转换。
     * @return 返回 clamp 计算得到的数量、金额或指标值。
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
     * @return 返回 default if blank 生成的文本或业务键。
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param task task 参数，用于 canView 流程中的校验、计算或对象转换。
     * @param user 操作人标识，用于审计和权限判断。
     * @return 返回布尔判断结果。
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

    /**
     * 当前用户上下文数据传输类型。
     * @param username 用户名.
     * @param role 角色编码.
     */
    private static final class CurrentUser {

        /**
         * username 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("username")
        private final String username;

        /**
         * role 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("role")
        private final String role;

        /**
         * 创建 CurrentUser 实例。
         *
         * @param username username 字段值
         * @param role role 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        private CurrentUser(@com.fasterxml.jackson.annotation.JsonProperty("username") String username, @com.fasterxml.jackson.annotation.JsonProperty("role") String role) {
            this.username = username;
            this.role = role;
        }

        /**
         * 返回username 字段值。
         *
         * @return username 字段值
         */
        public String username() {
            return username;
        }

        /**
         * 返回role 字段值。
         *
         * @return role 字段值
         */
        public String role() {
            return role;
        }

        /**
         * 判断两个 CurrentUser 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CurrentUser that)) {
                return false;
            }
            return java.util.Objects.equals(username, that.username) && java.util.Objects.equals(role, that.role);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(username, role);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "CurrentUser[" + "username=" + username + ", " + "role=" + role + "]";
        }
    }
}
