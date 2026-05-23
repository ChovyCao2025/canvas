package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStat;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
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

/**
 * 人群定义与计算管理接口。
 */
@RestController
@RequestMapping("/canvas/audiences")
@RequiredArgsConstructor
public class AudienceController {

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceStatMapper statMapper;
    private final AudienceBatchComputeService computeService;
    private final AudienceSchedulerService schedulerService;

    /** 分页查询人群定义。 */
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

    /** 查询单个人群定义详情。 */
    @GetMapping("/{id}")
    public Mono<R<AudienceDefinition>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(definitionMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 查询可用于实时判断的“READY 人群”列表。 */
    @GetMapping("/ready")
    public Mono<R<List<AudienceDefinition>>> listReady() {
        return Mono.fromCallable(() -> R.ok(computeService.listReadyDefinitions()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 创建人群并触发首次计算，同时注册调度任务。 */
    @PostMapping
    public Mono<R<AudienceDefinition>> create(@RequestBody AudienceDefinition body) {
        return Mono.fromCallable(() -> {
            AudienceDefinition created = computeService.create(body);
            schedulerService.refresh(created, () -> computeService.compute(created.getId()));
            return R.ok(created);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 更新人群并触发重算，同时刷新调度任务。 */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AudienceDefinition body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
            computeService.update(body);
            schedulerService.refresh(body, () -> computeService.compute(body.getId()));
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
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
    public Mono<R<Void>> compute(@PathVariable Long id) {
        return Mono.fromRunnable(() -> Thread.ofVirtual().start(() -> computeService.compute(id)))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    /** 查询人群计算状态统计。 */
    @GetMapping("/{id}/stat")
    public Mono<R<AudienceStat>> stat(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(statMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
