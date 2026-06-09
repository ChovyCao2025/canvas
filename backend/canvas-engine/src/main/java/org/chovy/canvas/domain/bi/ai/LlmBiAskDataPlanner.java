package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LlmBiAskDataPlanner 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class LlmBiAskDataPlanner implements BiAskDataPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 3L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    /**
     * 创建 LlmBiAskDataPlanner 实例并注入 domain.bi.ai 场景依赖。
     * @param llmGateway llm gateway 参数，用于 LlmBiAskDataPlanner 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LlmBiAskDataPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 使用 LLM 将自然语言取数问题规划为 BI 查询草案。
     *
     * <p>方法只向统一 LLM 网关提供问题、可选数据集 key 和数据集字段目录，并将输出 JSON 解析为维度、指标、过滤、
     * 排序和 limit；不直接执行查询，也不绕过后续查询编译和权限校验。</p>
     *
     * @param context 取数问题、租户、请求参数和可用数据集目录
     * @return LLM 状态、是否使用兜底输出以及查询规划草案
     */
    @Override
    public BiAskDataPlanningResult plan(BiAskDataPlanningContext context) {
        BiAskDataRequest request = context.request();
        AiLlmGateway.AiLlmResult result = llmGateway.evaluate(context.tenantId(), new AiLlmGateway.AiLlmRequest(
                request == null ? null : request.providerId(),
                request == null || request.templateId() == null ? DEFAULT_TEMPLATE_ID : request.templateId(),
                request == null ? null : request.modelKey(),
                null,
                variables(context),
                null,
                request == null ? Map.of() : request.params(),
                request == null ? null : request.timeoutMs(),
                null,
                null,
                "bi-ask-data-agent")).block();
        if (result == null) {
            throw new IllegalStateException("AI ask-data planner returned no result");
        }
        return new BiAskDataPlanningResult(result.status(), result.fallbackUsed(), toPlan(result.output()));
    }

    /**
     * 执行 variables 流程，围绕 variables 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 variables 流程生成的业务结果。
     */
    private JsonNode variables(BiAskDataPlanningContext context) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("question", context.question());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (context.requestedDatasetKey() == null) {
            variables.putNull("requestedDatasetKey");
        } else {
            variables.put("requestedDatasetKey", context.requestedDatasetKey());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        variables.set("datasets", objectMapper.valueToTree(context.datasets().stream()
                .map(this::datasetCatalog)
                .toList()));
        return variables;
    }

    /**
     * 将数据集规格压缩为 LLM 可消费的语义目录。
     *
     * <p>目录只暴露字段 key、字段角色、值类型、指标 key、指标值类型和允许维度，避免把底层 SQL 表达式、
     * 租户列或内部存储细节直接交给模型。</p>
     */
    private Map<String, Object> datasetCatalog(BiDatasetSpec dataset) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("datasetKey", dataset.datasetKey());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        catalog.put("fields", dataset.fields().values().stream()
                .sorted(Comparator.comparing(BiFieldSpec::fieldKey))
                .map(field -> Map.of(
                        "fieldKey", field.fieldKey(),
                        "role", field.role().name(),
                        "valueType", field.valueType()))
                .toList());
        catalog.put("metrics", dataset.metrics().values().stream()
                .sorted(Comparator.comparing(BiMetricSpec::metricKey))
                .map(metric -> Map.of(
                        "metricKey", metric.metricKey(),
                        "valueType", metric.valueType(),
                        "allowedDimensions", metric.allowedDimensions()))
                .toList());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return catalog;
    }

    /**
     * 将 LLM JSON 输出解析为问数计划。
     *
     * <p>缺失或非数组字段会归一化为空集合，limit 原样保留给上层执行请求再做边界裁剪；
     * 该方法不做字段权限判断，权限和字段白名单由 {@link BiAskDataAgentService} 与查询执行链路兜底。</p>
     */
    private BiAskDataPlan toPlan(JsonNode output) {
        JsonNode source = output == null || output.isNull() ? objectMapper.createObjectNode() : output;
        return new BiAskDataPlan(
                text(source, "datasetKey"),
                list(source.path("dimensions"), new TypeReference<>() {
                }),
                list(source.path("metrics"), new TypeReference<>() {
                }),
                list(source.path("filters"), new TypeReference<>() {
                }),
                list(source.path("sorts"), new TypeReference<>() {
                }),
                source.path("limit").asInt(0),
                text(source, "explanation"));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param node node 参数，用于 list 流程中的校验、计算或对象转换。
     * @param type 类型标识，用于选择对应处理分支。
     * @return 返回符合条件的数据列表或视图。
     */
    private <T> List<T> list(JsonNode node, TypeReference<List<T>> type) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(node, type);
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : null;
    }
}
