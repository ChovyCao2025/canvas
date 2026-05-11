package org.chovy.canvas.controller;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.dto.*;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService    canvasService;
    private final CanvasOpsService opsService;

    @PostMapping
    public Mono<R<Canvas>> create(@RequestBody CanvasCreateReq req) {
        return Mono.fromCallable(() -> canvasService.create(req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/{id}")
    public Mono<R<CanvasDetailDTO>> getById(@PathVariable Long id) {
        return Mono.fromCallable(() -> canvasService.getById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(detail -> detail != null ? R.ok(detail) : R.<CanvasDetailDTO>fail("画布不存在"));
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody CanvasUpdateReq req) {
        return Mono.<Void>fromRunnable(() -> canvasService.updateDraft(id, req))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    @GetMapping("/list")
    public Mono<R<PageResult<Canvas>>> list(CanvasListQuery query) {
        return Mono.fromCallable(() -> canvasService.list(query))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PostMapping("/{id}/publish")
    public Mono<R<CanvasVersion>> publish(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return Mono.fromCallable(() -> canvasService.publish(id, operator))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PostMapping("/{id}/offline")
    public Mono<R<Void>> offline(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return Mono.<Void>fromRunnable(() -> canvasService.offline(id, operator))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    @GetMapping("/{id}/versions")
    public Mono<R<List<CanvasVersion>>> getVersions(@PathVariable Long id) {
        return Mono.fromCallable(() -> canvasService.getVersions(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/{id}/versions/{versionId}")
    public Mono<R<CanvasVersion>> getVersion(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        return Mono.fromCallable(() -> canvasService.getVersion(id, versionId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(v -> v != null ? R.ok(v) : R.<CanvasVersion>fail("版本不存在"));
    }

    // ── 运营管控 ─────────────────────────────────────────────────

    @PostMapping("/{id}/kill")
    public Mono<R<Void>> kill(@PathVariable Long id,
                              @RequestParam(defaultValue = "GRACEFUL") String mode) {
        return Mono.<Void>fromRunnable(() -> opsService.kill(id, mode))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    @PostMapping("/{id}/canary")
    public Mono<R<Void>> startCanary(@PathVariable Long id,
                                     @RequestParam int percent) {
        return currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> opsService.startCanary(id, percent, operator))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    @PostMapping("/{id}/promote-canary")
    public Mono<R<Void>> promoteCanary(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> opsService.promoteCanary(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    @PostMapping("/{id}/rollback-canary")
    public Mono<R<Void>> rollbackCanary(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> opsService.rollbackCanary(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    @PostMapping("/{id}/rollback")
    public Mono<R<Void>> rollback(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> opsService.rollback(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    @PostMapping("/{id}/clone")
    public Mono<R<Canvas>> clone(@PathVariable Long id) {
        return currentUser().flatMap(operator ->
                Mono.fromCallable(() -> opsService.clone(id, operator))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    @GetMapping("/{id}/versions/{v1}/diff/{v2}")
    public Mono<R<Map<String, Object>>> diff(@PathVariable Long id,
                                             @PathVariable Long v1,
                                             @PathVariable Long v2) {
        return Mono.fromCallable(() -> opsService.diff(id, v1, v2))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /** PUT /canvas/{id} 升级：支持 editVersion 乐观锁 */
    @PutMapping("/{id}/safe")
    public Mono<R<Void>> safeUpdate(@PathVariable Long id,
                                    @RequestBody SafeUpdateReq req) {
        return currentUser().flatMap(operator ->
                Mono.<Void>fromRunnable(() -> opsService.saveWithOptimisticLock(
                        id, req.getName(), req.getDescription(),
                        req.getGraphJson(), req.getEditVersion(), operator))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok())
                        .onErrorResume(e -> {
                            if ("CANVAS_010".equals(e.getMessage()))
                                return Mono.just(R.fail("画布已被他人修改，请刷新后重试"));
                            return Mono.error(e);
                        }));
    }

    // ── helpers ───────────────────────────────────────────────────

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(io.jsonwebtoken.Claims.class)
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }

    @Data
    static class SafeUpdateReq {
        private String name;
        private String description;
        private String graphJson;
        private int editVersion;
    }
}
