package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStat;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.chovy.canvas.domain.task.AsyncTaskCreateResult;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.task.ComputeTaskResp;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
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

import java.util.List;

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

    @GetMapping
    public Mono<R<PageResult<AudienceDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Mono.fromCallable(() -> {
            Page<AudienceDefinition> result = definitionMapper.selectPage(
                    new Page<>(page, size),
                    new LambdaQueryWrapper<AudienceDefinition>().orderByDesc(AudienceDefinition::getId)
            );
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<R<AudienceDefinition>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(definitionMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/ready")
    public Mono<R<List<AudienceDefinition>>> listReady() {
        return Mono.fromCallable(() -> R.ok(computeService.listReadyDefinitions()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<AudienceDefinition>> create(@RequestBody AudienceDefinition body) {
        return currentUser().flatMap(operator ->
                Mono.fromCallable(() -> {
                    body.setCreatedBy(operator);
                    AudienceDefinition created = computeService.create(body);
                    schedulerService.refresh(created, () -> computeService.compute(created.getId()));
                    enqueueCompute(created, operator);
                    return R.ok(created);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AudienceDefinition body) {
        return currentUser().flatMap(operator ->
                Mono.fromCallable(() -> {
                    body.setId(id);
                    boolean updated = computeService.update(body);
                    if (!updated) {
                        throw new IllegalArgumentException("Audience not found: " + id);
                    }
                    AudienceDefinition saved = definitionMapper.selectById(id);
                    if (saved == null) {
                        throw new IllegalArgumentException("Audience not found: " + id);
                    }
                    schedulerService.refresh(saved, () -> computeService.compute(saved.getId()));
                    enqueueCompute(saved, operator);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            schedulerService.cancel(id);
            computeService.delete(id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/compute")
    public Mono<R<ComputeTaskResp>> compute(@PathVariable Long id) {
        return currentUser().flatMap(operator ->
                Mono.fromCallable(() -> {
                    AudienceDefinition definition = definitionMapper.selectById(id);
                    if (definition == null) {
                        throw new IllegalArgumentException("Audience not found: " + id);
                    }
                    if (definition.getEnabled() == null || definition.getEnabled() == 0) {
                        throw new IllegalStateException("Audience disabled: " + id);
                    }
                    return R.ok(enqueueCompute(definition, operator));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/stat")
    public Mono<R<AudienceStat>> stat(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(statMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ComputeTaskResp enqueueCompute(AudienceDefinition definition, String operator) {
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
        }
        return new ComputeTaskResp(taskId, result.task().getStatus());
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

    private String displayName(AudienceDefinition definition) {
        return defaultIfBlank(definition.getName(), "人群 " + definition.getId());
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
