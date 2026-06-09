package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.AbExperimentGovernanceService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * AbExperimentGovernanceController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/ab-experiments/{experimentId}/governance")
public class AbExperimentGovernanceController {

    private final AbExperimentGovernanceService service;

    /**
     * 创建 AbExperimentGovernanceController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AbExperimentGovernanceController(AbExperimentGovernanceService service) {
        this.service = service;
    }
    /**
     * 评估 A/B 实验 Governance接口，对应 POST /evaluate。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param experimentId experiment ID。
     * @param controlVariantKey control Variant 唯一键，默认值为 A。
     * @return 异步返回统一响应，包含评估 A/B 实验 Governance后的业务数据。
     */
    @PostMapping("/evaluate")
    public Mono<R<AbExperimentGovernanceService.Evaluation>> evaluate(
            @PathVariable Long experimentId,
            @RequestParam(defaultValue = "A") String controlVariantKey) {
        return Mono.fromCallable(() -> R.ok(service.evaluate(experimentId, controlVariantKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
