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

/**
 * MQ 消息定义管理接口。
 *
 * 该接口维护 messageCode 到 MQ 元信息的映射，
 * 供 `MQ_TRIGGER` / `SEND_MQ` 节点配置面板与运行时引用。
 */
@RestController
@RequestMapping("/canvas/mq-definitions")
@RequiredArgsConstructor
public class MqDefinitionController {

    /** MQ 消息定义 Mapper。 */
    private final MqMessageDefinitionMapper mapper;
    private final MqRouteRefreshService routeRefreshService;

    /** 分页查询 MQ 消息定义。 */
    @GetMapping
    public Mono<R<PageResult<MqMessageDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            // enabled 为空时不过滤，返回全部记录
            var wrapper = new LambdaQueryWrapper<MqMessageDefinition>()
                    .eq(enabled != null, MqMessageDefinition::getEnabled, enabled)
                    .orderByAsc(MqMessageDefinition::getId);
            Page<MqMessageDefinition> p = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 创建 MQ 消息定义。 */
    @PostMapping
    public Mono<R<MqMessageDefinition>> create(@RequestBody MqMessageDefinition body) {
        return Mono.fromCallable(() -> {
                    mapper.insert(body);
                    routeRefreshService.rebuildMqRoutes();
                    return R.ok(body);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 更新 MQ 消息定义。 */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody MqMessageDefinition body) {
        // 用 URL 参数覆盖 body.id，保证 REST 语义一致
        body.setId(id);
        return Mono.fromCallable(() -> {
                    mapper.updateById(body);
                    routeRefreshService.rebuildMqRoutes();
                    return R.<Void>ok();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 删除 MQ 消息定义。 */
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
