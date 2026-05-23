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

/**
 * 标签定义管理接口。
 *
 * 作用：
 * - 维护标签元数据（tagCode/tagName/tagType/启停）；
 * - 供标签节点配置时下拉选择，不直接参与标签值计算。
 */
@RestController
@RequestMapping("/canvas/tag-definitions")
@RequiredArgsConstructor
public class TagDefinitionController {

    /** 标签定义 Mapper。 */
    private final TagDefinitionMapper mapper;

    /** 分页查询标签定义。 */
    @GetMapping
    public Mono<R<PageResult<TagDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
            // 按需拼接筛选条件，避免前端未传参数导致“误过滤”
            var wrapper = new LambdaQueryWrapper<TagDefinition>()
                    .eq(tagType != null, TagDefinition::getTagType, tagType)
                    .eq(enabled != null, TagDefinition::getEnabled, enabled)
                    .orderByAsc(TagDefinition::getId);
            Page<TagDefinition> p = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 创建标签定义。 */
    @PostMapping
    public Mono<R<TagDefinition>> create(@RequestBody TagDefinition body) {
        return Mono.fromCallable(() -> {
                    mapper.insert(body);
                    return R.ok(body);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 更新标签定义。 */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagDefinition body) {
        // 与其他配置接口保持一致：路径参数优先
        body.setId(id);
        return Mono.fromCallable(() -> {
                    mapper.updateById(body);
                    return R.<Void>ok();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 删除标签定义。 */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<R<Void>>fromRunnable(() -> mapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.ok()));
    }
}
