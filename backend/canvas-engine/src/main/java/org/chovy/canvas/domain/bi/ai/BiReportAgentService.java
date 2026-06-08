package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BiReportAgentService 编排 domain.bi.ai 场景的领域业务规则。
 */
@Service
public class BiReportAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiReportPlanner planner;

    /**
     * 创建 BiReportAgentService 实例并注入 domain.bi.ai 场景依赖。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param planner planner 参数，用于 BiReportAgentService 流程中的校验、计算或对象转换。
     */
    public BiReportAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                BiReportPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    /**
     * 生成 BI 报告或仪表板草稿规划，保留数据集、字段和权限边界。
     *
     * @param request 查询、预览、嵌入或 AI 分析请求上下文
     * @param context BI 查询上下文，包含租户、用户、角色和权限治理信息
     * @return 根据请求生成的 BI 报告内容
     */
    public BiReportResponse generate(BiReportRequest request, BiQueryContext context) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("report request is required");
        }
        if (request.sections().isEmpty()) {
            throw new IllegalArgumentException("at least one report section is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        Map<String, BiDatasetSpec> datasets = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiReportSectionInput section : request.sections()) {
            if (section == null) {
                throw new IllegalArgumentException("report section is required");
            }
            BiDatasetSpec dataset = semanticValidator.validateQuery(section.query(), scopedContext.tenantId());
            semanticValidator.validateResultDataset(section.result(), dataset.datasetKey(), "section result");
            datasets.putIfAbsent(dataset.datasetKey(), dataset);
        }
        BiReportPlanningResult planning = planner.plan(new BiReportPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                List.copyOf(datasets.values()),
                request.sections()));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("report planner did not return a plan");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiReportResponse(
                planning.status(),
                planning.fallbackUsed(),
                textOr(planning.plan().title(), textOr(request.title(), "BI Report")),
                textOr(planning.plan().executiveSummary(), "No executive summary generated."),
                planning.plan().sections(),
                planning.plan().nextActions());
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
