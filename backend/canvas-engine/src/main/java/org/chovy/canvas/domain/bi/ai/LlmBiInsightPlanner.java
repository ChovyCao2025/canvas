package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

/**
 * LlmBiInsightPlanner 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class LlmBiInsightPlanner implements BiInsightPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 7L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    /**
     * 创建 LlmBiInsightPlanner 实例并注入 domain.bi.ai 场景依赖。
     * @param llmGateway llm gateway 参数，用于 LlmBiInsightPlanner 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LlmBiInsightPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 使用 LLM 为 BI 洞察请求生成解释和建议计划。
     *
     * <p>方法只提交问题、数据集、当前查询结果和基线结果给统一 LLM 网关，并把 JSON 输出转换为
     * {@link BiInsightPlan}；不重新计算指标，也不替代上游查询结果校验。</p>
     *
     * @param context 洞察请求、租户、数据集、当前结果和基线结果
     * @return LLM 状态、是否使用兜底输出以及结构化洞察计划
     */
    @Override
    public BiInsightPlanningResult plan(BiInsightPlanningContext context) {
        BiInsightRequest request = context.request();
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
                "bi-insight-agent")).block();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (result == null) {
            throw new IllegalStateException("AI insight planner returned no result");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiInsightPlanningResult(
                result.status(),
                result.fallbackUsed(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                objectMapper.convertValue(result.output(), BiInsightPlan.class));
    }

    /**
     * 执行 variables 流程，围绕 variables 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 variables 流程生成的业务结果。
     */
    private com.fasterxml.jackson.databind.JsonNode variables(BiInsightPlanningContext context) {
        // 准备本次处理所需的上下文和中间变量。
        BiInsightRequest request = context.request();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        var variables = objectMapper.createObjectNode();
        variables.put("question", value(request.question()));
        variables.set("dataset", objectMapper.valueToTree(context.dataset()));
        variables.set("query", objectMapper.valueToTree(context.query()));
        variables.set("currentResult", objectMapper.valueToTree(context.currentResult()));
        variables.set("baselineResult", objectMapper.valueToTree(context.baselineResult()));
        // 汇总前面计算出的状态和明细，返回给调用方。
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
