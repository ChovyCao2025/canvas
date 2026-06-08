package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

/**
 * BiInsightAgentService 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class BiInsightAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiInsightPlanner planner;

    /**
     * 创建 BiInsightAgentService 实例并注入 domain.bi.ai 场景依赖。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param planner planner 参数，用于 BiInsightAgentService 流程中的校验、计算或对象转换。
     */
    public BiInsightAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                 BiInsightPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    /**
     * 分析 BI 数据洞察请求，结合语义模型和查询上下文返回可行动的指标洞察。
     *
     * @param request 查询、预览、嵌入或 AI 分析请求上下文
     * @param context BI 查询上下文，包含租户、用户、角色和权限治理信息
     * @return 指标洞察分析结果
     */
    public BiInsightResponse inspect(BiInsightRequest request, BiQueryContext context) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("insight request is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        BiDatasetSpec dataset = semanticValidator.validateQuery(request.query(), scopedContext.tenantId());
        semanticValidator.validateResultDataset(request.currentResult(), dataset.datasetKey(), "current result");
        semanticValidator.validateResultDataset(request.baselineResult(), dataset.datasetKey(), "baseline result");
        BiInsightPlanningResult planning = planner.plan(new BiInsightPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                dataset,
                request.query(),
                request.currentResult(),
                request.baselineResult()));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("insight planner did not return a plan");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiInsightResponse(
                planning.status(),
                planning.fallbackUsed(),
                planning.plan().trends(),
                planning.plan().anomalies(),
                planning.plan().opportunities());
    }
}
