package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.AbExperiment;
import org.chovy.canvas.domain.meta.AbExperimentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/ab-experiments")
@RequiredArgsConstructor
public class AbExperimentController {

    private final AbExperimentMapper abExperimentMapper;

    @GetMapping
    public Mono<R<PageResult<AbExperiment>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<AbExperiment> wrapper = new LambdaQueryWrapper<AbExperiment>()
                    .orderByDesc(AbExperiment::getId);
            if (enabled != null) {
                wrapper.eq(AbExperiment::getEnabled, enabled);
            }
            Page<AbExperiment> pageResult = abExperimentMapper.selectPage(new Page<>(page, size), wrapper);
            return PageResult.of(pageResult.getTotal(), pageResult.getRecords());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    @PostMapping
    public Mono<R<AbExperiment>> create(@RequestBody AbExperiment body) {
        return Mono.fromCallable(() -> {
            if (body.getEnabled() == null) body.setEnabled(1);
            abExperimentMapper.insert(body);
            return body;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AbExperiment body) {
        return Mono.<Void>fromRunnable(() -> {
            body.setId(id);
            abExperimentMapper.updateById(body);
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.<Void>ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> abExperimentMapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }
}
