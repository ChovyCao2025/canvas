package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

/**
 * LlmBiReportPlanner 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class LlmBiReportPlanner implements BiReportPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 5L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    /**
     * 创建 LlmBiReportPlanner 实例并注入 domain.bi.ai 场景依赖。
     * @param llmGateway llm gateway 参数，用于 LlmBiReportPlanner 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LlmBiReportPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 使用 LLM 为 BI 报告生成结构化报告计划。
     *
     * <p>方法只把报告类型、标题、章节和数据集目录提交给统一 LLM 网关，并将符合模板 schema 的 JSON 输出转换为
     * {@link BiReportPlan}；不执行 SQL、不生成文件，也不保证内容已被人工审核。</p>
     *
     * @param context 报告请求、租户、章节和可用数据集上下文
     * @return LLM 状态、是否使用兜底输出以及结构化报告计划
     */
    @Override
    public BiReportPlanningResult plan(BiReportPlanningContext context) {
        BiReportRequest request = context.request();
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
                "bi-report-agent")).block();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (result == null) {
            throw new IllegalStateException("AI report planner returned no result");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiReportPlanningResult(
                result.status(),
                result.fallbackUsed(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                objectMapper.convertValue(result.output(), BiReportPlan.class));
    }

    /**
     * 执行 variables 流程，围绕 variables 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 variables 流程生成的业务结果。
     */
    private com.fasterxml.jackson.databind.JsonNode variables(BiReportPlanningContext context) {
        // 准备本次处理所需的上下文和中间变量。
        BiReportRequest request = context.request();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        var variables = objectMapper.createObjectNode();
        variables.put("reportType", value(request.reportType()));
        variables.put("title", value(request.title()));
        variables.set("sections", objectMapper.valueToTree(context.sections()));
        variables.set("datasets", objectMapper.valueToTree(context.datasets()));
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
