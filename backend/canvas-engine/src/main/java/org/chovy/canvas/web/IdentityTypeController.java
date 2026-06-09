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

    /** 身份类型服务，用于管理可导入身份标识。 */
    private final IdentityTypeService identityTypeService;
    /**
     * 查询Identity Type列表接口，对应 GET 请求。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 identityTypeService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param enabled 请求参数，可选。
     * @param allowImport 请求参数，可选。
     * @return 异步返回统一响应，包含分页结果。
     */
    @GetMapping
    public Mono<R<PageResult<IdentityTypeDO>>> list(
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) Integer allowImport) {
        return Mono.fromCallable(() -> {
            var rows = identityTypeService.list(enabled, allowImport);
            return R.ok(PageResult.of(rows.size(), rows));
        }).subscribeOn(Schedulers.boundedElastic());
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
    public Mono<R<IdentityTypeDO>> create(@RequestBody IdentityTypeDO body) {
        return Mono.fromCallable(() -> R.ok(identityTypeService.create(body)))
                .subscribeOn(Schedulers.boundedElastic());
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
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody IdentityTypeDO body) {
        return Mono.<Void>fromRunnable(() -> identityTypeService.update(id, body))
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
        return Mono.<Void>fromRunnable(() -> identityTypeService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
