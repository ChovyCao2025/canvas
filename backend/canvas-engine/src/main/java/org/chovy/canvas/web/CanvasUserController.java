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

    /** 画布用户查询服务，用于查询用户明细和画像字段。 */
    private final CanvasUserQueryService service;

    /**
     * 查询指定画布当前命中的用户清单。
     *
     * <p>接口以画布 ID 作为查询边界，返回用户在该画布下的基础画像和触达相关字段；
     * 租户访问控制由查询服务按画布归属处理，控制器只负责异步封装和统一响应。</p>
     *
     * @param id 画布 ID，用于限定用户清单所属的营销旅程。
     * @return 异步返回画布命中的用户行数据。
     */
    @GetMapping
    public Mono<R<List<CanvasUserRowDTO>>> list(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.listUsers(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询单个用户在指定画布中的画像和触达状态。
     *
     * <p>该接口用于画布用户明细页，服务层会把 {@code id} 与 {@code userId} 组合校验，
     * 避免读取不属于该画布命中集合的用户数据。</p>
     *
     * @param id 画布 ID。
     * @param userId 业务用户标识，通常来自 CDP 用户索引或触达事件。
     * @return 异步返回该用户在画布内的用户行数据。
     */
    @GetMapping("/{userId}")
    public Mono<R<CanvasUserRowDTO>> get(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.getUserInCanvas(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询单个用户在指定画布中的执行轨迹。
     *
     * <p>返回结果按服务层定义展示节点执行记录，可用于排查用户为什么进入、跳过或停留在某个节点。
     * 查询只读，不会重放节点或改变画布运行状态。</p>
     *
     * @param id 画布 ID。
     * @param userId 业务用户标识。
     * @return 异步返回该用户在画布内的执行记录列表。
     */
    @GetMapping("/{userId}/executions")
    public Mono<R<List<CanvasExecutionDO>>> executions(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.listExecutions(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
