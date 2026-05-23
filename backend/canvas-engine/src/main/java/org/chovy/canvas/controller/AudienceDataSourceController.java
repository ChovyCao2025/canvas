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
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/canvas/audience-data-sources")
@RequiredArgsConstructor
public class AudienceDataSourceController {

    private final AudienceDataSourceService service;

    @GetMapping
    public Mono<R<List<AudienceDataSource>>> list() {
        return Mono.fromCallable(() -> R.ok(mask(service.list())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<R<AudienceDataSource>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            AudienceDataSource dataSource = service.get(id);
            if (dataSource == null) {
                throw new ResponseStatusException(NOT_FOUND, "Audience data source not found: " + id);
            }
            return R.ok(mask(dataSource));
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<AudienceDataSource>> create(@RequestBody AudienceDataSource body) {
        return Mono.fromCallable(() -> R.ok(mask(service.create(body))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<AudienceDataSource>> update(@PathVariable Long id, @RequestBody AudienceDataSource body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
            AudienceDataSource dataSource = service.update(body);
            if (dataSource == null) {
                throw new ResponseStatusException(NOT_FOUND, "Audience data source not found: " + id);
            }
            return R.ok(mask(dataSource));
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            service.delete(id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<AudienceDataSource> mask(List<AudienceDataSource> dataSources) {
        List<AudienceDataSource> masked = new ArrayList<>(dataSources.size());
        for (AudienceDataSource dataSource : dataSources) {
            masked.add(mask(dataSource));
        }
        return masked;
    }

    private AudienceDataSource mask(AudienceDataSource dataSource) {
        if (dataSource == null) {
            return null;
        }
        AudienceDataSource masked = new AudienceDataSource();
        masked.setId(dataSource.getId());
        masked.setName(dataSource.getName());
        masked.setDescription(dataSource.getDescription());
        masked.setUrl(dataSource.getUrl());
        masked.setUsername(dataSource.getUsername());
        masked.setDriverClassName(dataSource.getDriverClassName());
        masked.setEnabled(dataSource.getEnabled());
        masked.setCreatedBy(dataSource.getCreatedBy());
        masked.setReferenceCount(dataSource.getReferenceCount());
        masked.setCreatedAt(dataSource.getCreatedAt());
        masked.setUpdatedAt(dataSource.getUpdatedAt());
        return masked;
    }
}
