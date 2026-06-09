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

    /** 标签导入来源服务，用于管理来源配置。 */
    private final TagImportSourceService tagImportSourceService;
    /**
     * 查询标签导入源列表接口，对应 GET 请求。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 tagImportSourceService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param enabled 请求参数，可选。
     * @return 异步返回统一响应，包含分页结果。
     */
    @GetMapping
    public Mono<R<PageResult<TagImportSourceDO>>> list(@RequestParam(required = false) Integer enabled) {
        return Mono.fromCallable(() -> {
                    List<TagImportSourceDO> sources = tagImportSourceService.list(enabled);
                    return PageResult.of(sources.size(), sources);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 处理 create 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping
    public Mono<R<TagImportSourceDO>> create(@RequestBody TagImportSourceDO body) {
        return Mono.fromCallable(() -> tagImportSourceService.create(body))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /**
     * 处理 update 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody TagImportSourceDO body) {
        return Mono.<Void>fromRunnable(() -> tagImportSourceService.update(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    /**
     * 处理 delete 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> tagImportSourceService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    /**
     * 处理 run 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/{id}/run")
    public Mono<R<TagImportResult>> run(@PathVariable Long id) {
        return tagImportSourceService.runAsync(id)
                .map(R::ok);
    }
}
