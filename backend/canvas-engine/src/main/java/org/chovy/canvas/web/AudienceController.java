package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.dataobject.AudienceStatDO;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.dal.dataobject.AsyncTaskDO;
import org.chovy.canvas.domain.task.AsyncTaskCreateResult;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.domain.task.AsyncTaskStatus;
import org.chovy.canvas.dto.audience.AudiencePreviewReq;
import org.chovy.canvas.dto.audience.AudiencePreviewResp;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.chovy.canvas.dto.task.ComputeTaskResp;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.chovy.canvas.engine.audience.CdpAudienceSourceService;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Audience HTTP 控制器，根路由为 {@code /canvas/audiences}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@Slf4j
@RestController
@RequestMapping("/canvas/audiences")
@RequiredArgsConstructor
public class AudienceController {

    /** 人群定义 Mapper，用于读写人群定义。 */
    private final AudienceDefinitionMapper definitionMapper;
    /** 人群统计 Mapper，用于读取人群统计。 */
    private final AudienceStatMapper statMapper;
    /** 人群批量计算服务，用于触发人群计算。 */
    private final AudienceBatchComputeService computeService;
    /** 人群调度服务，用于维护定时计算计划。 */
    private final AudienceSchedulerService schedulerService;
    /** 异步任务服务，用于登记和查询人群计算任务。 */
    private final AsyncTaskService taskService;
    /** 人群计算任务执行器，用于运行后台计算。 */
    private final AudienceComputeTaskRunner computeTaskRunner;
    /** 通知服务，用于发送人群任务通知。 */
    private final NotificationService notificationService;
    private final CdpAudienceSourceService cdpAudienceSourceService;

    /** 分页查询人群定义。 */
    @GetMapping
    public Mono<R<PageResult<AudienceDefinitionDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Mono.fromCallable(() -> {
            Page<AudienceDefinitionDO> result = definitionMapper.selectPage(
                    new Page<>(page, size),
                    new LambdaQueryWrapper<AudienceDefinitionDO>().orderByDesc(AudienceDefinitionDO::getId)
            );
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 查询 CDP 人群数据源可用于圈选的字段。 */
    @GetMapping("/source-fields")
    public Mono<R<List<AudienceSourceFieldDTO>>> sourceFields(@RequestParam String dataSourceType) {
        return Mono.fromCallable(() -> R.ok(cdpAudienceSourceService.listSourceFields(dataSourceType)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 预览 CDP 人群规则命中用户，不保存人群定义。 */
    @PostMapping("/preview")
    public Mono<R<AudiencePreviewResp>> preview(@RequestBody AudiencePreviewReq req) {
        return Mono.fromCallable(() -> {
            if (!cdpAudienceSourceService.supports(req.dataSourceType())) {
                throw new IllegalArgumentException("Unsupported CDP audience source: " + req.dataSourceType());
            }
            List<String> userIds = cdpAudienceSourceService.resolveUserIds(req.dataSourceType(), req.ruleJson());
            int limit = req.sampleLimit() == null ? 10 : Math.max(1, Math.min(req.sampleLimit(), 100));
            return R.ok(new AudiencePreviewResp(userIds.size(), userIds.stream().limit(limit).toList()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 查询单个人群定义详情。 */
    @GetMapping("/{id}")
    public Mono<R<AudienceDefinitionDO>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(definitionMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 查询可用于实时判断的“READY 人群”列表。 */
    @GetMapping("/ready")
    public Mono<R<List<AudienceDefinitionDO>>> listReady() {
        return Mono.fromCallable(() -> R.ok(computeService.listReadyDefinitions()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 创建人群并触发首次计算，同时注册调度任务。 */
    @PostMapping
    public Mono<R<AudienceDefinitionDO>> create(@RequestBody AudienceDefinitionDO body) {
        return currentUser().flatMap(operator ->
                Mono.fromCallable(() -> {
                    body.setCreatedBy(operator);
                    AudienceDefinitionDO created = computeService.create(body);
                    schedulerService.refresh(created, () -> computeService.compute(created.getId()));
                    enqueueCompute(created, operator);
                    return R.ok(created);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /** 更新人群并触发重算，同时刷新调度任务。 */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AudienceDefinitionDO body) {
        return currentUser().flatMap(operator ->
                Mono.fromCallable(() -> {
                    body.setId(id);
                    boolean updated = computeService.update(body);
                    if (!updated) {
                        throw new IllegalArgumentException("Audience not found: " + id);
                    }
                    AudienceDefinitionDO saved = definitionMapper.selectById(id);
                    if (saved == null) {
                        throw new IllegalArgumentException("Audience not found: " + id);
                    }
                    schedulerService.refresh(saved, () -> computeService.compute(saved.getId()));
                    enqueueCompute(saved, operator);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /** 删除人群定义、统计数据与调度任务。 */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            schedulerService.cancel(id);
            computeService.delete(id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 手动触发一次异步计算。 */
    @PostMapping("/{id}/compute")
    public Mono<R<ComputeTaskResp>> compute(@PathVariable Long id, @RequestBody(required = false) ComputeReq req) {
        String perfRunId = extractPerfRunId(req);
        if (perfRunId != null) {
            String perfInputId = req == null || req.getPerfInputId() == null || req.getPerfInputId().isBlank()
                    ? null
                    : req.getPerfInputId();
            return Mono.fromRunnable(() -> Thread.ofVirtual().start(
                            () -> computeService.compute(id, perfRunId, perfInputId)))
                    .subscribeOn(Schedulers.boundedElastic())
                    .thenReturn(R.ok(new ComputeTaskResp(perfTaskId(perfRunId, perfInputId), "QUEUED")));
        }

        return currentUser().flatMap(operator ->
                Mono.fromCallable(() -> {
                    AudienceDefinitionDO definition = definitionMapper.selectById(id);
                    if (definition == null) {
                        throw new IllegalArgumentException("Audience not found: " + id);
                    }
                    if (definition.getEnabled() == null || definition.getEnabled() == 0) {
                        throw new IllegalStateException("Audience disabled: " + id);
                    }
                    return R.ok(enqueueCompute(definition, operator));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 计算或统计 compute 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    public Mono<R<ComputeTaskResp>> compute(Long id) {
        return compute(id, null);
    }

    /** 查询人群计算状态统计。 */
    @GetMapping("/{id}/stat")
    public Mono<R<AudienceStatDO>> stat(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(statMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行 enqueue Compute 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param definition definition 方法执行所需的业务参数
     * @param operator operator 操作人标识
     * @return 方法执行后的业务结果
     */
    private ComputeTaskResp enqueueCompute(AudienceDefinitionDO definition, String operator) {
        String audienceId = String.valueOf(definition.getId());
        String displayName = displayName(definition);
        AsyncTaskCreateResult result = taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                audienceId,
                "计算人群：" + displayName,
                operator);
        String taskId = result.task().getTaskId();
        if (result.created()) {
            computeTaskRunner.start(taskId, definition.getId(), displayName, operator);
        } else {
            createCatchUpNotificationIfTerminal(result.task(), definition.getId(), displayName, operator);
        }
        return new ComputeTaskResp(taskId, result.task().getStatus());
    }

        /**
     * 创建或新增 create Catch Up Notification If Terminal 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param task task 方法执行所需的业务参数
     * @param audienceId audienceId 对应的业务主键或标识
     * @param displayName displayName 方法执行所需的业务参数
     * @param operator operator 操作人标识
     */
    private void createCatchUpNotificationIfTerminal(AsyncTaskDO task, Long audienceId, String displayName, String operator) {
        if (task == null || !isTerminal(task.getStatus())) {
            return;
        }
        String type = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus()) ? "TASK_SUCCEEDED" : "TASK_FAILED";
        String title = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus()) ? "人群计算完成" : "人群计算失败";
        String detail = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus())
                ? "任务已完成"
                : defaultIfBlank(task.getErrorMsg(), "计算失败");
        try {
            notificationService.createForTask(
                    operator,
                    type,
                    title,
                    displayName + " · " + detail,
                    "/audiences?highlight=" + audienceId + "&taskId=" + task.getTaskId(),
                    task.getTaskId());
        } catch (Exception e) {
            log.error("[AUDIENCE] failed to create catch-up notification taskId={} user={}: {}",
                    task.getTaskId(), operator, e.getMessage(), e);
        }
    }

    /**
     * 判断 is Terminal 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param status status 状态值或状态筛选条件
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean isTerminal(String status) {
        return AsyncTaskStatus.SUCCEEDED.name().equals(status)
                || AsyncTaskStatus.FAILED.name().equals(status)
                || AsyncTaskStatus.CANCELED.name().equals(status);
    }

    /**
     * 执行 current User 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                    if (ctx.getAuthentication() == null
                            || !(ctx.getAuthentication().getPrincipal() instanceof Claims claims)) {
                        return "system";
                    }
                    return defaultIfBlank(claims.get("username", String.class), "system");
                })
                .defaultIfEmpty("system");
    }

    /**
     * 执行 display Name 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param definition definition 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String displayName(AudienceDefinitionDO definition) {
        return defaultIfBlank(definition.getName(), "人群 " + definition.getId());
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
     * 执行 perf Task Id 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param perfRunId perfRunId 对应的业务主键或标识
     * @param perfInputId perfInputId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
    private String perfTaskId(String perfRunId, String perfInputId) {
        return "perf:" + defaultIfBlank(perfInputId, perfRunId);
    }

    /**
     * 执行 extract Perf Run Id 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param req 请求对象，承载调用方提交的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String extractPerfRunId(ComputeReq req) {
        if (req == null) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("perfRunId", req.getPerfRunId());
        return PerfRunContext.extract(payload);
    }

    @Data
    static class ComputeReq {
        /** 压测运行 ID。 */
        private String perfRunId;
        /** 压测输入 ID。 */
        private String perfInputId;
    }
}
