package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BiInterpretationAgentService 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class BiInterpretationAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiInterpretationPlanner planner;

    /**
     * 创建 BiInterpretationAgentService 实例并注入 domain.bi.ai 场景依赖。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param planner planner 参数，用于 BiInterpretationAgentService 流程中的校验、计算或对象转换。
     */
    public BiInterpretationAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                        BiInterpretationPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    /**
     * 解释 BI 图表或查询结果，结合语义上下文生成面向业务的洞察说明。
     *
     * @param request 查询、预览、嵌入或 AI 分析请求上下文
     * @param context BI 查询上下文，包含租户、用户、角色和权限治理信息
     * @return 面向业务用户的图表或查询结果解读
     */
    public BiInterpretationResponse interpret(BiInterpretationRequest request, BiQueryContext context) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("interpretation request is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        BiDatasetSpec dataset = semanticValidator.validateQuery(request.query(), scopedContext.tenantId());
        semanticValidator.validateResultDataset(request.result(), dataset.datasetKey(), "result");
        BiInterpretationPlanningResult planning = planner.plan(new BiInterpretationPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                List.of(dataset),
                request.query(),
                request.result()));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("interpretation planner did not return a plan");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiInterpretationResponse(
                planning.status(),
                planning.fallbackUsed(),
                textOr(planning.plan().summary(), "No interpretation generated."),
                planning.plan().keyFindings(),
                planning.plan().recommendations());
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
