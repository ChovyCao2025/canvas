package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.TagDefinition;
import org.chovy.canvas.domain.meta.TagDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/tag-definitions")
@RequiredArgsConstructor
public class TagDefinitionController {

    private final TagDefinitionMapper mapper;

    @GetMapping
    public Mono<R<PageResult<TagDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            var wrapper = new LambdaQueryWrapper<TagDefinition>()
                    .eq(tagType  != null, TagDefinition::getTagType,  tagType)
                    .eq(enabled  != null, TagDefinition::getEnabled,  enabled)
                    .orderByAsc(TagDefinition::getId);
            Page<TagDefinition> p = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<TagDefinition>> create(@RequestBody TagDefinition body) {
        return Mono.fromCallable(() -> { mapper.insert(body); return R.ok(body); })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagDefinition body) {
        body.setId(id);
        return Mono.fromCallable(() -> { mapper.updateById(body); return R.<Void>ok(); })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<R<Void>>fromRunnable(() -> mapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.ok()));
    }
}
