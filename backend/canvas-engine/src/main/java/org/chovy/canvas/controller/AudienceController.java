package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStat;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.chovy.canvas.perf.PerfRunContext;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/canvas/audiences")
@RequiredArgsConstructor
public class AudienceController {

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceStatMapper statMapper;
    private final AudienceBatchComputeService computeService;
    private final AudienceSchedulerService schedulerService;

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
        return Mono.fromCallable(() -> {
            AudienceDefinition created = computeService.create(body);
            schedulerService.refresh(created, () -> computeService.compute(created.getId()));
            return R.ok(created);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AudienceDefinition body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
            computeService.update(body);
            schedulerService.refresh(body, () -> computeService.compute(body.getId()));
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
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
    public Mono<R<Void>> compute(@PathVariable Long id, @RequestBody(required = false) ComputeReq req) {
        String perfRunId = extractPerfRunId(req);
        String perfInputId = req == null || req.getPerfInputId() == null || req.getPerfInputId().isBlank()
                ? null
                : req.getPerfInputId();
        return Mono.fromRunnable(() -> Thread.ofVirtual().start(
                        () -> computeService.compute(id, perfRunId, perfInputId)))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @GetMapping("/{id}/stat")
    public Mono<R<AudienceStat>> stat(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(statMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
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
