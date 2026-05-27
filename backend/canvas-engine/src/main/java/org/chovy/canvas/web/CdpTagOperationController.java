package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CdpTagOperationDO;
import org.chovy.canvas.domain.cdp.CdpTagOperationService;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;

/**
 * CDP 标签操作 HTTP 控制器，根路由为 {@code /cdp/tag-operations}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/cdp/tag-operations")
@RequiredArgsConstructor
public class CdpTagOperationController {

    /** CDP 标签操作服务，用于写入和查询标签变更记录。 */
    private final CdpTagOperationService service;

    /**
     * 处理 create 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param req 请求对象，承载调用方提交的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping
    public Mono<R<CdpTagOperationDO>> create(@RequestBody CdpBatchTagReq req) {
        return Mono.fromCallable(() -> R.ok(service.create(req)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping
    public Mono<R<List<CdpTagOperationDO>>> list(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int limit) {
        return Mono.fromCallable(() -> R.ok(service.listRecent(limit)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 get 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{id}")
    public Mono<R<CdpTagOperationDO>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.get(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 retry Failed 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/{id}/retry-failed")
    public Mono<R<CdpTagOperationDO>> retryFailed(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.retryFailed(id, null)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
