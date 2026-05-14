package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.ApiDefinition;
import org.chovy.canvas.domain.meta.ApiDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/api-definitions")
@RequiredArgsConstructor
public class ApiDefinitionController {

    private final ApiDefinitionMapper apiDefinitionMapper;

    @GetMapping
    public Mono<R<PageResult<ApiDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<ApiDefinition> wrapper = new LambdaQueryWrapper<ApiDefinition>()
                    .orderByDesc(ApiDefinition::getId);
            if (enabled != null) {
                wrapper.eq(ApiDefinition::getEnabled, enabled);
            }
            Page<ApiDefinition> pageResult = apiDefinitionMapper.selectPage(new Page<>(page, size), wrapper);
            return PageResult.of(pageResult.getTotal(), pageResult.getRecords());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    @PostMapping
    public Mono<R<ApiDefinition>> create(@RequestBody ApiDefinition body) {
        return Mono.fromCallable(() -> {
            if (body.getEnabled() == null) body.setEnabled(1);
            apiDefinitionMapper.insert(body);
            return body;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody ApiDefinition body) {
        return Mono.<Void>fromRunnable(() -> {
            body.setId(id);
            apiDefinitionMapper.updateById(body);
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.<Void>ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> apiDefinitionMapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }
}
