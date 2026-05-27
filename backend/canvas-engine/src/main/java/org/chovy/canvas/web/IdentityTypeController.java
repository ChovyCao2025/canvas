package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.IdentityTypeDO;
import org.chovy.canvas.domain.meta.IdentityTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 身份类型 HTTP 控制器，根路由为 {@code /canvas/identity-types}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/identity-types")
@RequiredArgsConstructor
public class IdentityTypeController {

    private final IdentityTypeService identityTypeService;

    @GetMapping
    public Mono<R<PageResult<IdentityTypeDO>>> list(
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) Integer allowImport) {
        return Mono.fromCallable(() -> {
            var rows = identityTypeService.list(enabled, allowImport);
            return R.ok(PageResult.of(rows.size(), rows));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<IdentityTypeDO>> create(@RequestBody IdentityTypeDO body) {
        return Mono.fromCallable(() -> R.ok(identityTypeService.create(body)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody IdentityTypeDO body) {
        return Mono.<Void>fromRunnable(() -> identityTypeService.update(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> identityTypeService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
