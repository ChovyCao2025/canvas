package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BiAskDataAgentService 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class BiAskDataAgentService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 10_000;

    private final BiDatasetSpecResolver datasetSpecResolver;
    private final BiAskDataPlanner planner;
    private final BiQueryExecutionService queryExecutionService;

    /**
     * 创建 BiAskDataAgentService 实例并注入 domain.bi.ai 场景依赖。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param planner planner 参数，用于 BiAskDataAgentService 流程中的校验、计算或对象转换。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiAskDataAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                 BiAskDataPlanner planner,
                                 BiQueryExecutionService queryExecutionService) {
        this.datasetSpecResolver = datasetSpecResolver == null
                ? BiDatasetSpecResolver.builtIn()
                : datasetSpecResolver;
        this.planner = planner;
        this.queryExecutionService = queryExecutionService;
    }

    /**
     * 执行自然语言问数流程，将用户问题规划为受权限和治理约束的 BI 查询响应。
     *
     * @param request 查询、预览、嵌入或 AI 分析请求上下文
     * @param context BI 查询上下文，包含租户、用户、角色和权限治理信息
     * @return 自然语言问数规划和查询回答结果
     */
    public BiAskDataResponse ask(BiAskDataRequest request, BiQueryContext context) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("ask-data request is required");
        }
        String question = requireQuestion(request.question());
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        List<BiDatasetSpec> datasets = semanticCatalog(request.datasetKey(), scopedContext.tenantId());
        BiAskDataPlanningResult planning = planner.plan(new BiAskDataPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                question,
                request.datasetKey(),
                datasets,
                request));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("ask-data planner did not return a plan");
        }
        BiQueryRequest query = toQuery(planning.plan(), datasets, request.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiQueryResult result = queryExecutionService.execute(query, scopedContext);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiAskDataResponse(
                question,
                planning.status(),
                planning.fallbackUsed(),
                normalizeExplanation(planning.plan().explanation()),
                query,
                result);
    }

    /**
     * 执行 semanticCatalog 流程，围绕 semantic catalog 完成校验、计算或结果组装。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 semantic catalog 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasetSpec> semanticCatalog(String datasetKey, Long tenantId) {
        String scopedDatasetKey = trimToNull(datasetKey);
        if (scopedDatasetKey != null) {
            return List.of(datasetSpecResolver.dataset(scopedDatasetKey, tenantId));
        }
        return datasetSpecResolver.datasets(tenantId);
    }

    /**
     * 将 LLM 输出计划转换为后端可执行的 BI 查询请求。
     *
     * <p>方法会在单数据集上下文中补齐缺失 datasetKey，并严格校验模型选择的数据集必须来自本次语义目录；
     * limit 同时受模型输出和用户请求约束，后续查询执行仍会继续执行权限、治理和缓存策略。</p>
     */
    private BiQueryRequest toQuery(BiAskDataPlan plan, List<BiDatasetSpec> datasets, int requestedLimit) {
        String datasetKey = trimToNull(plan.datasetKey());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (datasetKey == null && datasets.size() == 1) {
            datasetKey = datasets.get(0).datasetKey();
        }
        if (datasetKey == null) {
            throw new IllegalArgumentException("planner datasetKey is required");
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        Set<String> allowedDatasetKeys = datasets.stream()
                .map(BiDatasetSpec::datasetKey)
                .collect(Collectors.toSet());
        if (!allowedDatasetKeys.contains(datasetKey)) {
            throw new IllegalArgumentException("planner selected dataset outside semantic catalog: " + datasetKey);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQueryRequest(
                datasetKey,
                plan.dimensions(),
                plan.metrics(),
                plan.filters(),
                plan.sorts(),
                boundedLimit(plan.limit(), requestedLimit));
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param plannerLimit planner limit 参数，用于 boundedLimit 流程中的校验、计算或对象转换。
     * @param requestedLimit requested limit 参数，用于 boundedLimit 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int plannerLimit, int requestedLimit) {
        int plannerBound = normalizeLimit(plannerLimit);
        int requestBound = normalizeLimit(requestedLimit);
        return Math.min(plannerBound, requestBound);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(int value) {
        if (value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param question question 参数，用于 requireQuestion 流程中的校验、计算或对象转换。
     * @return 返回 require question 生成的文本或业务键。
     */
    private String requireQuestion(String question) {
        String value = trimToNull(question);
        if (value == null) {
            throw new IllegalArgumentException("question is required");
        }
        return value;
    }

    /**
     * 归一化 LLM 解释文本，避免响应中出现空说明。
     *
     * <p>默认说明只表示结果来自 BI 语义层，不代表查询已被人工审核或解释具备强因果结论。</p>
     */
    private String normalizeExplanation(String explanation) {
        String value = trimToNull(explanation);
        return value == null ? "Generated from BI semantic layer." : value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
