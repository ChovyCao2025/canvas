package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;
    private final ApiDefinitionMapper apiDefinitionMapper;
    private final AbExperimentMapper abExperimentMapper;

    @Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;

    @GetMapping("/node-types")
    public Mono<R<List<NodeTypeRegistry>>> getNodeTypes() {
        return Mono.fromCallable(metaService::getAllNodeTypes)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/node-types/{typeKey}/schema")
    public Mono<R<NodeTypeRegistry>> getNodeTypeSchema(@PathVariable String typeKey) {
        return Mono.fromCallable(() -> metaService.getNodeTypeSchema(typeKey))
                .subscribeOn(Schedulers.boundedElastic())
                .map(nt -> nt != null ? R.ok(nt) : R.<NodeTypeRegistry>fail("节点类型不存在: " + typeKey));
    }

    @GetMapping("/context-fields")
    public Mono<R<List<ContextField>>> getContextFields() {
        return Mono.fromCallable(metaService::getAllContextFields)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/mq-topics")
    public Mono<R<List<StubOption>>> getMqTopics() {
        return Mono.just(R.ok(metaService.getMqTopics()));
    }

    @GetMapping("/coupon-types")
    public Mono<R<List<StubOption>>> getCouponTypes() {
        return Mono.just(R.ok(metaService.getCouponTypes()));
    }

    @GetMapping("/reach-scenes")
    public Mono<R<List<StubOption>>> getReachScenes() {
        return Mono.just(R.ok(metaService.getReachScenes()));
    }

    @GetMapping("/ab-experiments")
    public Mono<R<List<StubOption>>> getAbExperiments() {
        return Mono.fromCallable(() -> {
            List<AbExperiment> experiments = abExperimentMapper.selectList(
                    new LambdaQueryWrapper<AbExperiment>()
                            .eq(AbExperiment::getEnabled, 1)
                            .orderByAsc(AbExperiment::getId)
            );
            return experiments.stream()
                    .map(e -> new StubOption(e.getExperimentKey(), e.getName()))
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    @GetMapping("/ab-experiments/{key}/groups")
    public Mono<R<List<StubOption>>> getAbExperimentGroups(@PathVariable String key) {
        return Mono.just(R.ok(metaService.getAbExperimentGroups(key)));
    }

    @GetMapping("/api-definitions")
    public Mono<R<List<StubOption>>> getApiDefinitions() {
        return Mono.fromCallable(() -> {
            List<ApiDefinition> defs = apiDefinitionMapper.selectList(
                    new LambdaQueryWrapper<ApiDefinition>()
                            .eq(ApiDefinition::getEnabled, 1)
                            .orderByAsc(ApiDefinition::getId)
            );
            return defs.stream()
                    .map(def -> new StubOption(String.valueOf(def.getApiKey()), def.getName()))
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 动态获取 Tagger 标签列表（代理调 Tagger 服务）。
     * Tagger 服务不可用时降级返回 stub 数据，不影响页面加载。
     */
    @GetMapping("/tagger-tags")
    @SuppressWarnings("unchecked")
    public Mono<R<List<StubOption>>> getTaggerTags(
            @RequestParam(defaultValue = "realtime") String type) {
        return WebClient.builder().baseUrl(taggerUrl).build()
                .get()
                .uri(u -> u.path("/tags").queryParam("type", type).build())
                .retrieve()
                .bodyToFlux(Map.class)
                .map(m -> new StubOption(
                        String.valueOf(m.getOrDefault("code", "")),
                        String.valueOf(m.getOrDefault("name", ""))))
                .collectList()
                .map(R::ok)
                .onErrorResume(e -> {
                    log.warn("[META] Tagger 标签拉取失败，降级返回 stub: {}", e.getMessage());
                    return Mono.just(R.ok(metaService.getTaggerTags(type)));
                });
    }

    @GetMapping("/biz-lines")
    public Mono<R<List<StubOption>>> getBizLines() {
        return Mono.just(R.ok(metaService.getBizLines()));
    }

    @GetMapping("/biz-lines/{key}/apis")
    public Mono<R<List<StubOption>>> getBizLineApis(@PathVariable String key) {
        return Mono.just(R.ok(metaService.getBizLineApis(key)));
    }

    @GetMapping("/behavior-strategy-types")
    public Mono<R<List<StubOption>>> getBehaviorStrategyTypes() {
        return Mono.just(R.ok(metaService.getBehaviorStrategyTypes()));
    }

    @GetMapping("/message-codes")
    public Mono<R<List<StubOption>>> getMessageCodes(
            @RequestParam(defaultValue = "IN_APP") String type) {
        return Mono.just(R.ok(metaService.getMessageCodes(type)));
    }
}
