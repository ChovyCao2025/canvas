package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.TagImportSource;
import org.chovy.canvas.domain.meta.TagImportSourceService;
import org.chovy.canvas.dto.TagImportResult;
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

@RestController
@RequestMapping("/canvas/tag-import-sources")
@RequiredArgsConstructor
public class TagImportSourceController {

    private final TagImportSourceService tagImportSourceService;

    @GetMapping
    public Mono<R<PageResult<TagImportSource>>> list(@RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
                    List<TagImportSource> sources = tagImportSourceService.list(enabled);
                    return PageResult.of(sources.size(), sources);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PostMapping
    public Mono<R<TagImportSource>> create(@RequestBody TagImportSource body) {
        return Mono.fromCallable(() -> tagImportSourceService.create(body))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagImportSource body) {
        return Mono.<Void>fromRunnable(() -> tagImportSourceService.update(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> tagImportSourceService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @PostMapping("/{id}/run")
    public Mono<R<TagImportResult>> run(@PathVariable Long id) {
        return Mono.fromCallable(() -> tagImportSourceService.run(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
}
