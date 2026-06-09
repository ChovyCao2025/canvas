package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDependencyDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagRunDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpComputedTagDefinitionMapper;
import org.chovy.canvas.dal.mapper.CdpComputedTagDependencyMapper;
import org.chovy.canvas.dal.mapper.CdpComputedTagRunMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * ComputedTagService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class ComputedTagService {
    private static final Set<String> SUPPORTED_VALUE_TYPES = Set.of("STRING", "NUMBER", "BOOLEAN", "JSON");
    private static final Set<String> SUPPORTED_COMPUTE_TYPES = Set.of("RULE", "EXPR", "SQL");

    private final CdpComputedTagDefinitionMapper definitionMapper;
    private final CdpComputedTagDependencyMapper dependencyMapper;
    private final CdpComputedTagRunMapper runMapper;
    private final CdpUserProfileMapper profileMapper;
    private final CdpTagService cdpTagService;
    private final CdpRuleEvaluator ruleEvaluator;

    /**
     * DefinitionCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DefinitionCommand(
            String tagCode,
            String displayName,
            String valueType,
            String computeType,
            String expressionJson,
            String refreshMode,
            List<String> dependencies
    ) {
    }

    /**
     * PreviewSample 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreviewSample(String userId, String tagValue) {
    }

    /**
     * PreviewResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreviewResult(long scannedCount, long matchedCount, List<PreviewSample> samples) {
    }

    /**
     * RunResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RunResult(Long runId, String status, long scannedCount, long matchedCount,
                            long updatedCount, long skippedCount, long failedCount, String cyclePath) {
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<CdpComputedTagDefinitionDO> list(Long tenantId) {
        return definitionMapper.selectList(new LambdaQueryWrapper<CdpComputedTagDefinitionDO>()
                .eq(CdpComputedTagDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                .orderByDesc(CdpComputedTagDefinitionDO::getUpdatedAt));
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param createdBy created by 参数，用于 create 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public CdpComputedTagDefinitionDO create(Long tenantId, DefinitionCommand command, String createdBy) {
        CdpComputedTagDefinitionDO row = new CdpComputedTagDefinitionDO();
        row.setTenantId(normalizeTenantId(tenantId));
        row.setTagCode(requireText(command.tagCode(), "tagCode"));
        row.setDisplayName(command.displayName() == null || command.displayName().isBlank()
                ? row.getTagCode()
                : command.displayName().trim());
        row.setValueType(validateEnum(defaultText(command.valueType(), "BOOLEAN").toUpperCase(), "valueType", SUPPORTED_VALUE_TYPES));
        row.setComputeType(validateEnum(defaultText(command.computeType(), "RULE").toUpperCase(), "computeType", SUPPORTED_COMPUTE_TYPES));
        row.setExpressionJson(requireText(command.expressionJson(), "expressionJson"));
        validateExpression(row.getExpressionJson(), row.getComputeType());
        row.setRefreshMode(defaultText(command.refreshMode(), "MANUAL"));
        row.setStatus(CdpComputedTagDefinitionDO.DRAFT);
        row.setCreatedBy(createdBy);
        definitionMapper.insert(row);
        replaceDependencies(row.getTenantId(), row.getTagCode(), command.dependencies());
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     */
    public void activate(Long tenantId, String tagCode) {
        CdpComputedTagDefinitionDO row = requireDefinition(tenantId, tagCode, false);
        validateDefinition(row);
        String cycle = findCyclePath(row.getTagCode(), loadGraph(row.getTenantId()));
        if (cycle != null) {
            throw new IllegalArgumentException("computed tag dependency cycle: " + cycle);
        }
        row.setStatus(CdpComputedTagDefinitionDO.ACTIVE);
        definitionMapper.updateById(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     */
    public void pause(Long tenantId, String tagCode) {
        CdpComputedTagDefinitionDO row = requireDefinition(tenantId, tagCode, false);
        row.setStatus(CdpComputedTagDefinitionDO.PAUSED);
        definitionMapper.updateById(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 preview 流程生成的业务结果。
     */
    public PreviewResult preview(Long tenantId, String tagCode) {
        CdpComputedTagDefinitionDO row = requireDefinition(tenantId, tagCode, false);
        EvaluationSummary summary = evaluate(row, false, null, null);
        return new PreviewResult(summary.scannedCount, summary.matchedCount, summary.samples);
    }

    @Transactional
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public RunResult runNow(Long tenantId, String tagCode, String operator) {
        // 准备本次处理所需的上下文和中间变量。
        CdpComputedTagDefinitionDO row = requireDefinition(tenantId, tagCode, true);
        CdpComputedTagRunDO run = startRun(row.getTenantId(), row.getTagCode());
        try {
            EvaluationSummary summary = evaluate(row, true, run.getId(), operator);
            run.setStatus(CdpComputedTagRunDO.SUCCESS);
            run.setScannedCount(summary.scannedCount);
            run.setMatchedCount(summary.matchedCount);
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            run.setUpdatedCount(summary.updatedCount);
            run.setSkippedCount(summary.skippedCount);
            run.setFailedCount(summary.failedCount);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new RunResult(run.getId(), run.getStatus(), summary.scannedCount, summary.matchedCount,
                    summary.updatedCount, summary.skippedCount, summary.failedCount, run.getCyclePath());
        } catch (RuntimeException ex) {
            run.setStatus(CdpComputedTagRunDO.FAILED);
            run.setErrorMessage(ex.getMessage());
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            throw ex;
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<CdpComputedTagRunDO> listRuns(Long tenantId, String tagCode, Integer limit) {
        Long normalizedTenantId = normalizeTenantId(tenantId);
        return runMapper.selectList(new LambdaQueryWrapper<CdpComputedTagRunDO>()
                .eq(CdpComputedTagRunDO::getTenantId, normalizedTenantId)
                .eq(CdpComputedTagRunDO::getTagCode, requireText(tagCode, "tagCode"))
                .orderByDesc(CdpComputedTagRunDO::getStartedAt)
                .last("LIMIT " + normalizeLimit(limit)));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param mutate mutate 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param runId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    private EvaluationSummary evaluate(CdpComputedTagDefinitionDO row, boolean mutate, Long runId, String operator) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CdpUserProfileDO> profiles = profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getTenantId, row.getTenantId())
                .eq(CdpUserProfileDO::getStatus, "ACTIVE"));
        EvaluationSummary summary = new EvaluationSummary();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpUserProfileDO profile : profiles) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!row.getTenantId().equals(profile.getTenantId())) {
                continue;
            }
            summary.scannedCount++;
            Map<String, Object> properties = new LinkedHashMap<>(ruleEvaluator.readProperties(profile.getPropertiesJson()));
            if ("SQL".equalsIgnoreCase(row.getComputeType())) {
                throw new IllegalStateException("SQL computed tags are metadata-only in this slice");
            }
            CdpRuleEvaluator.Evaluation evaluation = ruleEvaluator.evaluate(row.getExpressionJson(), row.getComputeType(), properties);
            if (!evaluation.matched()) {
                summary.skippedCount++;
                continue;
            }
            summary.matchedCount++;
            String tagValue = stringify(evaluation.value());
            if (summary.samples.size() < 20) {
                summary.samples.add(new PreviewSample(profile.getUserId(), tagValue));
            }
            if (mutate) {
                try {
                    cdpTagService.setTag(row.getTenantId(), profile.getUserId(), new CdpTagWriteReq(
                            row.getTagCode(),
                            tagValue,
                            "computed tag run " + runId,
                            null,
                            "COMPUTED_TAG",
                            String.valueOf(runId),
                            operator,
                            "computed-tag:" + runId + ":" + profile.getUserId() + ":" + row.getTagCode()));
                    summary.updatedCount++;
                } catch (RuntimeException ex) {
                    summary.failedCount++;
                }
            }
        }
        return summary;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 startRun 流程生成的业务结果。
     */
    private CdpComputedTagRunDO startRun(Long tenantId, String tagCode) {
        CdpComputedTagRunDO run = new CdpComputedTagRunDO();
        run.setTenantId(tenantId);
        run.setTagCode(tagCode);
        run.setStatus(CdpComputedTagRunDO.RUNNING);
        run.setScannedCount(0L);
        run.setMatchedCount(0L);
        run.setUpdatedCount(0L);
        run.setSkippedCount(0L);
        run.setFailedCount(0L);
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);
        return run;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param activeOnly active only 参数，用于 requireDefinition 流程中的校验、计算或对象转换。
     * @return 返回 requireDefinition 流程生成的业务结果。
     */
    private CdpComputedTagDefinitionDO requireDefinition(Long tenantId, String tagCode, boolean activeOnly) {
        CdpComputedTagDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<CdpComputedTagDefinitionDO>()
                .eq(CdpComputedTagDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                .eq(CdpComputedTagDefinitionDO::getTagCode, requireText(tagCode, "tagCode"))
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("computed tag not found: " + tagCode);
        }
        if (activeOnly && !CdpComputedTagDefinitionDO.ACTIVE.equals(row.getStatus())) {
            throw new IllegalStateException("computed tag is not ACTIVE");
        }
        return row;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     */
    private void validateDefinition(CdpComputedTagDefinitionDO row) {
        validateEnum(defaultText(row.getValueType(), "BOOLEAN").toUpperCase(), "valueType", SUPPORTED_VALUE_TYPES);
        validateEnum(defaultText(row.getComputeType(), "RULE").toUpperCase(), "computeType", SUPPORTED_COMPUTE_TYPES);
        validateExpression(row.getExpressionJson(), row.getComputeType());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expressionJson JSON 字符串，承载结构化配置或明细。
     * @param computeType 类型标识，用于选择对应处理分支。
     */
    private void validateExpression(String expressionJson, String computeType) {
        if ("SQL".equalsIgnoreCase(computeType)) {
            Map<String, Object> expression = ruleEvaluator.readProperties(expressionJson);
            Object sql = expression.get("sql");
            requireText(sql instanceof String value ? value : null, "sql");
            return;
        }
        ruleEvaluator.validate(expressionJson, computeType);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 load graph 汇总后的集合、分页或映射视图。
     */
    private Map<String, List<String>> loadGraph(Long tenantId) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (CdpComputedTagDependencyDO edge : dependencyMapper.selectList(new LambdaQueryWrapper<CdpComputedTagDependencyDO>()
                .eq(CdpComputedTagDependencyDO::getTenantId, tenantId))) {
            graph.computeIfAbsent(edge.getTagCode(), ignored -> new ArrayList<>())
                    .add(edge.getDependsOnTagCode());
        }
        return graph;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param startTag start tag 参数，用于 findCyclePath 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 findCyclePath 流程中的校验、计算或对象转换。
     * @param graph graph 参数，用于 findCyclePath 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private String findCyclePath(String startTag, Map<String, List<String>> graph) {
        return dfs(startTag, graph, new HashSet<>(), new ArrayDeque<>());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tag 待处理业务值，用于规则计算、转换或外部调用。
     * @param MapString map string 参数，用于 dfs 流程中的校验、计算或对象转换。
     * @param graph graph 参数，用于 dfs 流程中的校验、计算或对象转换。
     * @param visiting visiting 参数，用于 dfs 流程中的校验、计算或对象转换。
     * @param stack stack 参数，用于 dfs 流程中的校验、计算或对象转换。
     * @return 返回 dfs 生成的文本或业务键。
     */
    private String dfs(String tag, Map<String, List<String>> graph, Set<String> visiting, ArrayDeque<String> stack) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (visiting.contains(tag)) {
            List<String> cycle = new ArrayList<>(stack);
            int start = cycle.indexOf(tag);
            List<String> path = new ArrayList<>(cycle.subList(Math.max(start, 0), cycle.size()));
            path.add(tag);
            return String.join(" -> ", path);
        }
        visiting.add(tag);
        stack.addLast(tag);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String next : graph.getOrDefault(tag, List.of())) {
            String cycle = dfs(next, graph, visiting, stack);
            if (cycle != null) {
                return cycle;
            }
        }
        stack.removeLast();
        visiting.remove(tag);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param dependencies dependencies 参数，用于 replaceDependencies 流程中的校验、计算或对象转换。
     */
    private void replaceDependencies(Long tenantId, String tagCode, List<String> dependencies) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dependencies == null) {
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String dependency : dependencies.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList()) {
            CdpComputedTagDependencyDO edge = new CdpComputedTagDependencyDO();
            edge.setTenantId(tenantId);
            edge.setTagCode(tagCode);
            edge.setDependsOnTagCode(dependency);
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            dependencyMapper.insert(edge);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 stringify 生成的文本或业务键。
     */
    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultText 流程中的校验、计算或对象转换。
     * @return 返回 default text 生成的文本或业务键。
     */
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @param supported supported 参数，用于 validateEnum 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private String validateEnum(String value, String fieldName, Set<String> supported) {
        if (!supported.contains(value)) {
            throw new IllegalArgumentException(fieldName + " is not supported: " + value);
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }

    /**
     * EvaluationSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class EvaluationSummary {
        long scannedCount;
        long matchedCount;
        long updatedCount;
        long skippedCount;
        long failedCount;
        final List<PreviewSample> samples = new ArrayList<>();
    }
}
