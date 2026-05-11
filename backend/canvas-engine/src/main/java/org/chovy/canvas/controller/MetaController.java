package org.chovy.canvas.controller;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

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
        return Mono.just(R.ok(metaService.getAbExperiments()));
    }

    @GetMapping("/ab-experiments/{key}/groups")
    public Mono<R<List<StubOption>>> getAbExperimentGroups(@PathVariable String key) {
        return Mono.just(R.ok(metaService.getAbExperimentGroups(key)));
    }

    @GetMapping("/tagger-tags")
    public Mono<R<List<StubOption>>> getTaggerTags(
            @RequestParam(defaultValue = "realtime") String type) {
        return Mono.just(R.ok(metaService.getTaggerTags(type)));
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
