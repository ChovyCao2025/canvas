package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.audience.AudienceDataSource;
import org.chovy.canvas.engine.audience.AudienceDataSourceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/audience-data-sources")
@RequiredArgsConstructor
public class AudienceDataSourceController {

    private final AudienceDataSourceService service;

    @GetMapping
    public Mono<R<List<AudienceDataSource>>> list() {
        return Mono.fromCallable(() -> R.ok(service.list()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<AudienceDataSource>> create(@RequestBody AudienceDataSource body) {
        return Mono.fromCallable(() -> R.ok(service.create(body)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<AudienceDataSource>> update(@PathVariable Long id, @RequestBody AudienceDataSource body) {
        body.setId(id);
        return Mono.fromCallable(() -> R.ok(service.update(body)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            service.delete(id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
