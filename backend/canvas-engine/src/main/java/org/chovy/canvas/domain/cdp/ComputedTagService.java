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
public class ComputedTagService {
    private static final Set<String> SUPPORTED_VALUE_TYPES = Set.of("STRING", "NUMBER", "BOOLEAN", "JSON");
    private static final Set<String> SUPPORTED_COMPUTE_TYPES = Set.of("RULE", "EXPR", "SQL");

    private final CdpComputedTagDefinitionMapper definitionMapper;
    private final CdpComputedTagDependencyMapper dependencyMapper;
    private final CdpComputedTagRunMapper runMapper;
    private final CdpUserProfileMapper profileMapper;
    private final CdpTagService cdpTagService;
    private final CdpRuleEvaluator ruleEvaluator;

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

    public record PreviewSample(String userId, String tagValue) {
    }

    public record PreviewResult(long scannedCount, long matchedCount, List<PreviewSample> samples) {
    }

    public record RunResult(Long runId, String status, long scannedCount, long matchedCount,
                            long updatedCount, long skippedCount, long failedCount, String cyclePath) {
    }

    public List<CdpComputedTagDefinitionDO> list(Long tenantId) {
        return definitionMapper.selectList(new LambdaQueryWrapper<CdpComputedTagDefinitionDO>()
                .eq(CdpComputedTagDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                .orderByDesc(CdpComputedTagDefinitionDO::getUpdatedAt));
    }

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

    public void pause(Long tenantId, String tagCode) {
        CdpComputedTagDefinitionDO row = requireDefinition(tenantId, tagCode, false);
        row.setStatus(CdpComputedTagDefinitionDO.PAUSED);
        definitionMapper.updateById(row);
    }

    public PreviewResult preview(Long tenantId, String tagCode) {
        CdpComputedTagDefinitionDO row = requireDefinition(tenantId, tagCode, false);
        EvaluationSummary summary = evaluate(row, false, null, null);
        return new PreviewResult(summary.scannedCount, summary.matchedCount, summary.samples);
    }

    @Transactional
    public RunResult runNow(Long tenantId, String tagCode, String operator) {
        CdpComputedTagDefinitionDO row = requireDefinition(tenantId, tagCode, true);
        CdpComputedTagRunDO run = startRun(row.getTenantId(), row.getTagCode());
        try {
            EvaluationSummary summary = evaluate(row, true, run.getId(), operator);
            run.setStatus(CdpComputedTagRunDO.SUCCESS);
            run.setScannedCount(summary.scannedCount);
            run.setMatchedCount(summary.matchedCount);
            run.setUpdatedCount(summary.updatedCount);
            run.setSkippedCount(summary.skippedCount);
            run.setFailedCount(summary.failedCount);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
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

    public List<CdpComputedTagRunDO> listRuns(Long tenantId, String tagCode, Integer limit) {
        Long normalizedTenantId = normalizeTenantId(tenantId);
        return runMapper.selectList(new LambdaQueryWrapper<CdpComputedTagRunDO>()
                .eq(CdpComputedTagRunDO::getTenantId, normalizedTenantId)
                .eq(CdpComputedTagRunDO::getTagCode, requireText(tagCode, "tagCode"))
                .orderByDesc(CdpComputedTagRunDO::getStartedAt)
                .last("LIMIT " + normalizeLimit(limit)));
    }

    private EvaluationSummary evaluate(CdpComputedTagDefinitionDO row, boolean mutate, Long runId, String operator) {
        List<CdpUserProfileDO> profiles = profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getTenantId, row.getTenantId())
                .eq(CdpUserProfileDO::getStatus, "ACTIVE"));
        EvaluationSummary summary = new EvaluationSummary();
        for (CdpUserProfileDO profile : profiles) {
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

    private void validateDefinition(CdpComputedTagDefinitionDO row) {
        validateEnum(defaultText(row.getValueType(), "BOOLEAN").toUpperCase(), "valueType", SUPPORTED_VALUE_TYPES);
        validateEnum(defaultText(row.getComputeType(), "RULE").toUpperCase(), "computeType", SUPPORTED_COMPUTE_TYPES);
        validateExpression(row.getExpressionJson(), row.getComputeType());
    }

    private void validateExpression(String expressionJson, String computeType) {
        if ("SQL".equalsIgnoreCase(computeType)) {
            Map<String, Object> expression = ruleEvaluator.readProperties(expressionJson);
            Object sql = expression.get("sql");
            requireText(sql instanceof String value ? value : null, "sql");
            return;
        }
        ruleEvaluator.validate(expressionJson, computeType);
    }

    private Map<String, List<String>> loadGraph(Long tenantId) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (CdpComputedTagDependencyDO edge : dependencyMapper.selectList(new LambdaQueryWrapper<CdpComputedTagDependencyDO>()
                .eq(CdpComputedTagDependencyDO::getTenantId, tenantId))) {
            graph.computeIfAbsent(edge.getTagCode(), ignored -> new ArrayList<>())
                    .add(edge.getDependsOnTagCode());
        }
        return graph;
    }

    private String findCyclePath(String startTag, Map<String, List<String>> graph) {
        return dfs(startTag, graph, new HashSet<>(), new ArrayDeque<>());
    }

    private String dfs(String tag, Map<String, List<String>> graph, Set<String> visiting, ArrayDeque<String> stack) {
        if (visiting.contains(tag)) {
            List<String> cycle = new ArrayList<>(stack);
            int start = cycle.indexOf(tag);
            List<String> path = new ArrayList<>(cycle.subList(Math.max(start, 0), cycle.size()));
            path.add(tag);
            return String.join(" -> ", path);
        }
        visiting.add(tag);
        stack.addLast(tag);
        for (String next : graph.getOrDefault(tag, List.of())) {
            String cycle = dfs(next, graph, visiting, stack);
            if (cycle != null) {
                return cycle;
            }
        }
        stack.removeLast();
        visiting.remove(tag);
        return null;
    }

    private void replaceDependencies(Long tenantId, String tagCode, List<String> dependencies) {
        if (dependencies == null) {
            return;
        }
        for (String dependency : dependencies.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList()) {
            CdpComputedTagDependencyDO edge = new CdpComputedTagDependencyDO();
            edge.setTenantId(tenantId);
            edge.setTagCode(tagCode);
            edge.setDependsOnTagCode(dependency);
            dependencyMapper.insert(edge);
        }
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String validateEnum(String value, String fieldName, Set<String> supported) {
        if (!supported.contains(value)) {
            throw new IllegalArgumentException(fieldName + " is not supported: " + value);
        }
        return value;
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }

    private static final class EvaluationSummary {
        long scannedCount;
        long matchedCount;
        long updatedCount;
        long skippedCount;
        long failedCount;
        final List<PreviewSample> samples = new ArrayList<>();
    }
}
