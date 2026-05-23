package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.MqMessageDefinition;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.chovy.canvas.infra.redis.MqRouteRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/mq-definitions")
@RequiredArgsConstructor
public class MqDefinitionController {

    private final MqMessageDefinitionMapper mapper;
    private final MqRouteRefreshService routeRefreshService;

    @GetMapping
    public Mono<R<PageResult<MqMessageDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            var wrapper = new LambdaQueryWrapper<MqMessageDefinition>()
                    .eq(enabled != null, MqMessageDefinition::getEnabled, enabled)
                    .orderByAsc(MqMessageDefinition::getId);
            Page<MqMessageDefinition> p = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<MqMessageDefinition>> create(@RequestBody MqMessageDefinition body) {
        return Mono.fromCallable(() -> {
                    mapper.insert(body);
                    routeRefreshService.rebuildMqRoutes();
                    return R.ok(body);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody MqMessageDefinition body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
                    mapper.updateById(body);
                    routeRefreshService.rebuildMqRoutes();
                    return R.<Void>ok();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<R<Void>>fromRunnable(() -> {
                    mapper.deleteById(id);
                    routeRefreshService.rebuildMqRoutes();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.ok()));
    }
}
