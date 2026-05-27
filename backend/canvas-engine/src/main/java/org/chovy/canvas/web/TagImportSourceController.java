package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.TagImportSourceDO;
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

/**
 * 标签导入来源 HTTP 控制器，根路由为 {@code /canvas/tag-import-sources}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/tag-import-sources")
@RequiredArgsConstructor
public class TagImportSourceController {

    private final TagImportSourceService tagImportSourceService;

    @GetMapping
    public Mono<R<PageResult<TagImportSourceDO>>> list(@RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
                    List<TagImportSourceDO> sources = tagImportSourceService.list(enabled);
                    return PageResult.of(sources.size(), sources);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PostMapping
    public Mono<R<TagImportSourceDO>> create(@RequestBody TagImportSourceDO body) {
        return Mono.fromCallable(() -> tagImportSourceService.create(body))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagImportSourceDO body) {
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
