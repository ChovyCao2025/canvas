package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.TagImportBatch;
import org.chovy.canvas.domain.meta.TagImportError;
import org.chovy.canvas.domain.meta.TagImportService;
import org.chovy.canvas.dto.TagImportPushReq;
import org.chovy.canvas.dto.TagImportResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/tag-imports")
@RequiredArgsConstructor
public class TagImportController {

    private final TagImportService tagImportService;

    @PostMapping("/api-push")
    public Mono<R<TagImportResult>> apiPush(@RequestBody TagImportPushReq req) {
        return Mono.fromCallable(() -> tagImportService.importRows("API_PUSH", null, null, req.getRows()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/batches")
    public Mono<R<List<TagImportBatch>>> listBatches() {
        return Mono.fromCallable(tagImportService::listBatches)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @GetMapping("/batches/{id}/errors")
    public Mono<R<List<TagImportError>>> listErrors(@PathVariable("id") Long batchId) {
        return Mono.fromCallable(() -> tagImportService.listErrors(batchId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
}
