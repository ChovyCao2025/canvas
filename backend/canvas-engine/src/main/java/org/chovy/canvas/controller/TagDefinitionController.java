package org.chovy.canvas.controller;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.TagDefinition;
import org.chovy.canvas.domain.meta.TagDefinitionService;
import org.chovy.canvas.domain.meta.TagValueDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/canvas/tag-definitions")
@RequiredArgsConstructor
public class TagDefinitionController {

    private final TagDefinitionService tagDefinitionService;

    @GetMapping
    public Mono<R<PageResult<TagDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            List<TagDefinition> all = tagDefinitionService.list(tagType, enabled);
            int fromIndex = Math.max(0, (page - 1) * size);
            if (fromIndex >= all.size()) {
                return PageResult.of(all.size(), Collections.<TagDefinition>emptyList());
            }
            int toIndex = Math.min(all.size(), fromIndex + size);
            return PageResult.of(all.size(), all.subList(fromIndex, toIndex));
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    @PostMapping
    public Mono<R<TagDefinition>> create(@RequestBody TagDefinition body) {
        return Mono.fromCallable(() -> {
                    return R.ok(tagDefinitionService.create(body));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagDefinition body) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.update(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @GetMapping("/{tagCode}/values")
    public Mono<R<List<TagValueDefinition>>> listValues(
            @PathVariable String tagCode,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> tagDefinitionService.listValues(tagCode, enabled))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PostMapping("/{tagCode}/values")
    public Mono<R<TagValueDefinition>> createValue(@PathVariable String tagCode, @RequestBody TagValueDefinition body) {
        return Mono.fromCallable(() -> tagDefinitionService.createValue(tagCode, body))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/values/{id}")
    public Mono<R<Void>> updateValue(@PathVariable Long id, @RequestBody TagValueDefinition body) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.updateValue(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @DeleteMapping("/values/{id}")
    public Mono<R<Void>> deleteValue(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> tagDefinitionService.deleteValue(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
