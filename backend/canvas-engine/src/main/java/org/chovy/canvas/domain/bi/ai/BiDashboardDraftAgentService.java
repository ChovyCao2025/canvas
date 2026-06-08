package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BiDashboardDraftAgentService 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class BiDashboardDraftAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiDashboardDraftPlanner planner;

    /**
     * 创建 BiDashboardDraftAgentService 实例并注入 domain.bi.ai 场景依赖。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param planner planner 参数，用于 BiDashboardDraftAgentService 流程中的校验、计算或对象转换。
     */
    public BiDashboardDraftAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                        BiDashboardDraftPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    /**
     * 生成 BI 报告或仪表板草稿规划，保留数据集、字段和权限边界。
     *
     * @param request 查询、预览、嵌入或 AI 分析请求上下文
     * @param context BI 查询上下文，包含租户、用户、角色和权限治理信息
     * @return 根据请求生成的仪表板草稿方案
     */
    public BiDashboardDraftResponse generate(BiDashboardDraftRequest request, BiQueryContext context) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("dashboard draft request is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        List<BiDatasetSpec> datasets = semanticValidator.catalog(request.datasetKey(), scopedContext.tenantId());
        BiDashboardDraftPlanningResult planning = planner.plan(new BiDashboardDraftPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                datasets));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("dashboard draft planner did not return a plan");
        }
        semanticValidator.validateDashboardDraft(planning.plan(), scopedContext.tenantId());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDashboardDraftResponse(
                planning.status(),
                planning.fallbackUsed(),
                planning.plan().dashboard(),
                planning.plan().charts(),
                textOr(planning.plan().explanation(), "Generated dashboard draft from BI semantic layer."));
    }

    /**
     * 执行 textOr 流程，围绕 text or 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 textOr 流程中的校验、计算或对象转换。
     * @return 返回 text or 生成的文本或业务键。
     */
    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
