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
import org.chovy.canvas.dto.task.ComputeTaskResp;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
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

@Slf4j
@RestController
@RequestMapping("/canvas/audiences")
@RequiredArgsConstructor
public class AudienceController {

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceStatMapper statMapper;
    private final AudienceBatchComputeService computeService;
    private final AudienceSchedulerService schedulerService;
    private final AsyncTaskService taskService;
    private final AudienceComputeTaskRunner computeTaskRunner;
    private final NotificationService notificationService;

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

    public Mono<R<ComputeTaskResp>> compute(Long id) {
        return compute(id, null);
    }

    /** 查询人群计算状态统计。 */
    @GetMapping("/{id}/stat")
    public Mono<R<AudienceStatDO>> stat(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(statMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

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

    private boolean isTerminal(String status) {
        return AsyncTaskStatus.SUCCEEDED.name().equals(status)
                || AsyncTaskStatus.FAILED.name().equals(status)
                || AsyncTaskStatus.CANCELED.name().equals(status);
    }

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

    private String displayName(AudienceDefinitionDO definition) {
        return defaultIfBlank(definition.getName(), "人群 " + definition.getId());
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String perfTaskId(String perfRunId, String perfInputId) {
        return "perf:" + defaultIfBlank(perfInputId, perfRunId);
    }

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
        private String perfRunId;
        private String perfInputId;
    }
}
