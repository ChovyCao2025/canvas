package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CanvasUserQueryService;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dto.cdp.CanvasUserRowDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 画布用户 HTTP 控制器，根路由为 {@code /canvas/{id}/users}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/{id}/users")
@RequiredArgsConstructor
public class CanvasUserController {

    private final CanvasUserQueryService service;

    @GetMapping
    public Mono<R<List<CanvasUserRowDTO>>> list(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.listUsers(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}")
    public Mono<R<CanvasUserRowDTO>> get(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.getUserInCanvas(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/executions")
    public Mono<R<List<CanvasExecutionDO>>> executions(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.listExecutions(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
