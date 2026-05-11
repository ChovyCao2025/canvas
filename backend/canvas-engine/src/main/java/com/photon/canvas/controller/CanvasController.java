package com.photon.canvas.controller;

import com.photon.canvas.common.PageResult;
import com.photon.canvas.common.R;
import com.photon.canvas.domain.canvas.Canvas;
import com.photon.canvas.domain.canvas.CanvasService;
import com.photon.canvas.domain.canvas.CanvasVersion;
import com.photon.canvas.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService canvasService;

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
}
