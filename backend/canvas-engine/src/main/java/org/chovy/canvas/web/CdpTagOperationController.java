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

    private final CdpTagOperationService service;

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

    @GetMapping("/{id}")
    public Mono<R<CdpTagOperationDO>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.get(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/retry-failed")
    public Mono<R<CdpTagOperationDO>> retryFailed(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.retryFailed(id, null)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
