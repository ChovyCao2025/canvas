package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

/**
 * LlmBiDashboardDraftPlanner 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class LlmBiDashboardDraftPlanner implements BiDashboardDraftPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 6L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    /**
     * 创建 LlmBiDashboardDraftPlanner 实例并注入 domain.bi.ai 场景依赖。
     * @param llmGateway llm gateway 参数，用于 LlmBiDashboardDraftPlanner 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LlmBiDashboardDraftPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 使用 LLM 为自然语言仪表盘需求生成草稿计划。
     *
     * <p>方法只将用户提示、期望数据集和数据集目录发送到统一 LLM 网关，并把返回 JSON 转换为
     * {@link BiDashboardDraftPlan}；不创建仪表盘记录，也不执行任何图表查询。</p>
     *
     * @param context 仪表盘草稿请求、租户和可用数据集目录
     * @return LLM 状态、是否使用兜底输出以及仪表盘草稿计划
     */
    @Override
    public BiDashboardDraftPlanningResult plan(BiDashboardDraftPlanningContext context) {
        BiDashboardDraftRequest request = context.request();
        AiLlmGateway.AiLlmResult result = llmGateway.evaluate(context.tenantId(), new AiLlmGateway.AiLlmRequest(
                request.providerId(),
                request.templateId() == null ? DEFAULT_TEMPLATE_ID : request.templateId(),
                request.modelKey(),
                null,
                variables(context),
                request.params(),
                request.timeoutMs(),
                null,
                null,
                "bi-dashboard-draft-agent")).block();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (result == null) {
            throw new IllegalStateException("AI dashboard draft planner returned no result");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDashboardDraftPlanningResult(
                result.status(),
                result.fallbackUsed(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                objectMapper.convertValue(result.output(), BiDashboardDraftPlan.class));
    }

    /**
     * 执行 variables 流程，围绕 variables 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 variables 流程生成的业务结果。
     */
    private com.fasterxml.jackson.databind.JsonNode variables(BiDashboardDraftPlanningContext context) {
        BiDashboardDraftRequest request = context.request();
        var variables = objectMapper.createObjectNode();
        variables.put("prompt", value(request.prompt()));
        variables.put("requestedDatasetKey", value(request.datasetKey()));
        variables.set("datasets", objectMapper.valueToTree(context.datasets()));
        return variables;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 生成的文本或业务键。
     */
    private String value(String value) {
        return value == null ? "" : value;
    }
}
