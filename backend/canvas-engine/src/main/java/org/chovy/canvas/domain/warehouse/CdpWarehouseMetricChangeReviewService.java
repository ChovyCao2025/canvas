package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiMetricDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseMetricChangeReviewDO;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseMetricChangeReviewMapper;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CdpWarehouseMetricChangeReviewService {

    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_APPLIED = "APPLIED";

    private static final String CHANGE_TYPE = "METRIC_CONTRACT";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";
    private static final Pattern METRIC_EXPRESSION = Pattern.compile("[A-Za-z0-9_\\s().,+\\-*/<>=]+");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final CdpWarehouseMetricChangeReviewMapper reviewMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiMetricMapper metricMapper;
    private final BiDatasetSpecResolver datasetSpecResolver;
    private final CdpWarehouseMetricLineageService metricLineageService;
    private final ObjectMapper objectMapper;

    public CdpWarehouseMetricChangeReviewService(
            CdpWarehouseMetricChangeReviewMapper reviewMapper,
            BiDatasetMapper datasetMapper,
            BiMetricMapper metricMapper,
            ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
            CdpWarehouseMetricLineageService metricLineageService,
            ObjectMapper objectMapper) {
        this(
                reviewMapper,
                datasetMapper,
                metricMapper,
                datasetSpecResolverProvider == null
                        ? BiDatasetSpecResolver.builtIn()
                        : datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                metricLineageService,
                objectMapper);
    }

    CdpWarehouseMetricChangeReviewService(CdpWarehouseMetricChangeReviewMapper reviewMapper,
                                          BiDatasetMapper datasetMapper,
                                          BiMetricMapper metricMapper,
                                          BiDatasetSpecResolver datasetSpecResolver,
                                          CdpWarehouseMetricLineageService metricLineageService,
                                          ObjectMapper objectMapper) {
        this.reviewMapper = reviewMapper;
        this.datasetMapper = datasetMapper;
        this.metricMapper = metricMapper;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.metricLineageService = metricLineageService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public List<MetricChangeReviewView> list(Long tenantId,
                                             String datasetKey,
                                             String metricKey,
                                             String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseMetricChangeReviewDO> query =
                new LambdaQueryWrapper<CdpWarehouseMetricChangeReviewDO>()
                        .eq(CdpWarehouseMetricChangeReviewDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehouseMetricChangeReviewDO::getCreatedAt)
                        .orderByDesc(CdpWarehouseMetricChangeReviewDO::getId);
        if (hasText(datasetKey)) {
            query.eq(CdpWarehouseMetricChangeReviewDO::getDatasetKey, datasetKey.trim());
        }
        if (hasText(metricKey)) {
            query.eq(CdpWarehouseMetricChangeReviewDO::getMetricKey, metricKey.trim());
        }
        if (hasText(status)) {
            query.eq(CdpWarehouseMetricChangeReviewDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        return safeList(reviewMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    public MetricChangeReviewView requestChange(Long tenantId,
                                                String requestedBy,
                                                MetricChangeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("metric change command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String datasetKey = required(command.datasetKey(), "datasetKey");
        String metricKey = required(command.metricKey(), "metricKey");
        String proposedExpression = validateExpression(command.proposedExpression());
        String reason = required(command.reason(), "reason");
        rejectConflictingOpenReview(scopedTenantId, datasetKey, metricKey);

        BiDatasetSpec dataset = datasetSpecResolver.dataset(datasetKey, scopedTenantId);
        if (dataset == null) {
            throw new IllegalArgumentException("Unknown dataset: " + datasetKey);
        }
        BiMetricSpec currentMetric = dataset.metrics().get(metricKey);
        if (currentMetric == null) {
            throw new IllegalArgumentException("Unknown metric: " + metricKey);
        }
        List<String> proposedAllowedDimensions = validateAllowedDimensions(
                dataset, command.proposedAllowedDimensions());
        MetricSnapshot currentSnapshot = new MetricSnapshot(
                metricKey,
                currentMetric.expression(),
                currentMetric.valueType(),
                currentMetric.allowedDimensions());
        MetricSnapshot proposedSnapshot = new MetricSnapshot(
                metricKey,
                proposedExpression,
                currentMetric.valueType(),
                proposedAllowedDimensions);
        CdpWarehouseMetricLineageService.MetricImpactView impact =
                metricLineageService.impact(scopedTenantId, datasetKey, metricKey);
        ImpactSummary impactSummary = toImpactSummary(impact);

        CdpWarehouseMetricChangeReviewDO row = new CdpWarehouseMetricChangeReviewDO();
        row.setTenantId(scopedTenantId);
        row.setDatasetKey(datasetKey);
        row.setMetricKey(metricKey);
        row.setChangeType(CHANGE_TYPE);
        row.setCurrentSnapshotJson(json(currentSnapshot));
        row.setProposedSnapshotJson(json(proposedSnapshot));
        row.setImpactSummaryJson(json(impactSummary));
        row.setRiskLevel(riskLevel(impactSummary));
        row.setStatus(STATUS_PENDING_REVIEW);
        row.setRequestedBy(defaultActor(requestedBy));
        row.setRequestReason(reason);
        reviewMapper.insert(row);
        return toView(row);
    }

    public MetricChangeReviewView approve(Long tenantId, Long reviewId, String reviewer, String note) {
        CdpWarehouseMetricChangeReviewDO row = reviewForUpdate(tenantId, reviewId);
        requireStatus(row, STATUS_PENDING_REVIEW, "only pending metric changes can be approved");
        row.setStatus(STATUS_APPROVED);
        row.setReviewedBy(defaultActor(reviewer));
        row.setReviewNote(required(note, "reviewNote"));
        row.setReviewedAt(LocalDateTime.now());
        reviewMapper.updateById(row);
        return toView(row);
    }

    public MetricChangeReviewView reject(Long tenantId, Long reviewId, String reviewer, String note) {
        CdpWarehouseMetricChangeReviewDO row = reviewForUpdate(tenantId, reviewId);
        requireStatus(row, STATUS_PENDING_REVIEW, "only pending metric changes can be rejected");
        row.setStatus(STATUS_REJECTED);
        row.setReviewedBy(defaultActor(reviewer));
        row.setReviewNote(required(note, "reviewNote"));
        row.setReviewedAt(LocalDateTime.now());
        reviewMapper.updateById(row);
        return toView(row);
    }

    public MetricChangeReviewView apply(Long tenantId, Long reviewId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseMetricChangeReviewDO row = reviewForUpdate(scopedTenantId, reviewId);
        requireStatus(row, STATUS_APPROVED, "only approved metric changes can be applied");

        MetricSnapshot currentSnapshot = metricSnapshot(row.getCurrentSnapshotJson());
        MetricSnapshot proposedSnapshot = metricSnapshot(row.getProposedSnapshotJson());
        BiDatasetDO dataset = persistedDataset(scopedTenantId, row.getDatasetKey());
        if (dataset == null) {
            throw new IllegalStateException("persisted BI dataset is required before applying metric change: "
                    + row.getDatasetKey());
        }
        BiMetricDO metric = persistedMetric(scopedTenantId, dataset.getId(), row.getMetricKey());
        if (metric == null) {
            throw new IllegalStateException("persisted BI metric is required before applying metric change: "
                    + row.getMetricKey());
        }
        ensureCurrentStillMatches(currentSnapshot, metric);

        int updated = metricMapper.updateMetricContract(
                scopedTenantId,
                dataset.getId(),
                row.getMetricKey(),
                proposedSnapshot.expression(),
                json(proposedSnapshot.allowedDimensions()));
        if (updated <= 0) {
            throw new IllegalStateException("metric change was not applied: " + row.getMetricKey());
        }
        row.setStatus(STATUS_APPLIED);
        row.setAppliedAt(LocalDateTime.now());
        reviewMapper.updateById(row);
        return toView(row);
    }

    private void rejectConflictingOpenReview(Long tenantId, String datasetKey, String metricKey) {
        List<CdpWarehouseMetricChangeReviewDO> openRows = safeList(reviewMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseMetricChangeReviewDO>()
                        .eq(CdpWarehouseMetricChangeReviewDO::getTenantId, tenantId)
                        .eq(CdpWarehouseMetricChangeReviewDO::getDatasetKey, datasetKey)
                        .eq(CdpWarehouseMetricChangeReviewDO::getMetricKey, metricKey)
                        .in(CdpWarehouseMetricChangeReviewDO::getStatus,
                                List.of(STATUS_PENDING_REVIEW, STATUS_APPROVED))
                        .last("LIMIT 1")));
        if (!openRows.isEmpty()) {
            throw new IllegalStateException("metric change already has an open review: " + metricKey);
        }
    }

    private List<String> validateAllowedDimensions(BiDatasetSpec dataset, List<String> proposedAllowedDimensions) {
        if (proposedAllowedDimensions == null || proposedAllowedDimensions.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String dimension : proposedAllowedDimensions) {
            String fieldKey = required(dimension, "allowedDimension");
            BiFieldSpec field = dataset.fields().get(fieldKey);
            if (field == null) {
                throw new IllegalArgumentException("metric allowed dimension is not a dataset field: " + fieldKey);
            }
            if (field.role() != BiFieldSpec.Role.DIMENSION) {
                throw new IllegalArgumentException("metric allowed dimension is not a dimension field: " + fieldKey);
            }
            normalized.add(fieldKey);
        }
        return List.copyOf(normalized);
    }

    private String validateExpression(String value) {
        String expression = required(value, "proposedExpression");
        if (!METRIC_EXPRESSION.matcher(expression).matches()
                || expression.contains("--")
                || expression.contains("/*")
                || expression.contains(";")) {
            throw new IllegalArgumentException("metric expression contains unsafe characters");
        }
        return expression;
    }

    private CdpWarehouseMetricChangeReviewDO reviewForUpdate(Long tenantId, Long reviewId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (reviewId == null) {
            throw new IllegalArgumentException("reviewId is required");
        }
        CdpWarehouseMetricChangeReviewDO row = reviewMapper.selectOne(
                new LambdaQueryWrapper<CdpWarehouseMetricChangeReviewDO>()
                        .eq(CdpWarehouseMetricChangeReviewDO::getTenantId, scopedTenantId)
                        .eq(CdpWarehouseMetricChangeReviewDO::getId, reviewId)
                        .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("metric change review not found: " + reviewId);
        }
        return row;
    }

    private BiDatasetDO persistedDataset(Long tenantId, String datasetKey) {
        return datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, tenantId)
                .eq(BiDatasetDO::getDatasetKey, datasetKey)
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .last("LIMIT 1"));
    }

    private BiMetricDO persistedMetric(Long tenantId, Long datasetId, String metricKey) {
        return metricMapper.selectOne(new LambdaQueryWrapper<BiMetricDO>()
                .eq(BiMetricDO::getTenantId, tenantId)
                .eq(BiMetricDO::getDatasetId, datasetId)
                .eq(BiMetricDO::getMetricKey, metricKey)
                .ne(BiMetricDO::getStatus, STATUS_ARCHIVED)
                .last("LIMIT 1"));
    }

    private void ensureCurrentStillMatches(MetricSnapshot currentSnapshot, BiMetricDO metric) {
        if (!currentSnapshot.expression().equals(metric.getExpression())
                || !currentSnapshot.allowedDimensions().equals(stringList(metric.getAllowedDimensionsJson()))) {
            throw new IllegalStateException("metric changed since review was requested: " + metric.getMetricKey());
        }
    }

    private void requireStatus(CdpWarehouseMetricChangeReviewDO row, String expectedStatus, String message) {
        if (!expectedStatus.equals(row.getStatus())) {
            throw new IllegalStateException(message);
        }
    }

    private ImpactSummary toImpactSummary(CdpWarehouseMetricLineageService.MetricImpactView impact) {
        CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage = impact.transitiveLineage();
        return new ImpactSummary(
                impact.fieldDependencies().size(),
                impact.lineageNodes().size(),
                impact.lineageEdges().size(),
                transitiveLineage == null ? 0 : transitiveLineage.nodes().size(),
                transitiveLineage == null ? 0 : transitiveLineage.edges().size(),
                transitiveLineage == null ? 0 : transitiveLineage.paths().size(),
                transitiveLineage == null ? 0 : downstreamNodeCount(transitiveLineage),
                transitiveLineage != null && transitiveLineage.truncated(),
                impact.charts().size(),
                impact.dashboards().size(),
                impact.warnings());
    }

    private String riskLevel(ImpactSummary impactSummary) {
        if (impactSummary.dashboardCount() > 0
                || impactSummary.transitiveTruncated()
                || impactSummary.transitiveDownstreamNodeCount() > 0) {
            return RISK_HIGH;
        }
        if (impactSummary.chartCount() > 0
                || impactSummary.fieldDependencyCount() > 0
                || impactSummary.lineageEdgeCount() > 0
                || impactSummary.transitiveLineageEdgeCount() > 0) {
            return RISK_MEDIUM;
        }
        return RISK_LOW;
    }

    private int downstreamNodeCount(CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage) {
        return (int) transitiveLineage.nodes().stream()
                .filter(node -> node.relation() == CdpWarehouseCatalogService.LineageRelation.DOWNSTREAM
                        || node.relation() == CdpWarehouseCatalogService.LineageRelation.BOTH)
                .count();
    }

    private MetricChangeReviewView toView(CdpWarehouseMetricChangeReviewDO row) {
        return new MetricChangeReviewView(
                row.getId(),
                row.getTenantId(),
                row.getDatasetKey(),
                row.getMetricKey(),
                metricSnapshot(row.getCurrentSnapshotJson()),
                metricSnapshot(row.getProposedSnapshotJson()),
                impactSummary(row.getImpactSummaryJson()),
                row.getRiskLevel(),
                row.getStatus(),
                row.getRequestedBy(),
                row.getRequestReason(),
                row.getReviewedBy(),
                row.getReviewedAt(),
                row.getReviewNote(),
                row.getAppliedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private MetricSnapshot metricSnapshot(String value) {
        return fromJson(value, MetricSnapshot.class);
    }

    private ImpactSummary impactSummary(String value) {
        return fromJson(value, ImpactSummary.class);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize metric change review payload", e);
        }
    }

    private <T> T fromJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to read metric change review payload", e);
        }
    }

    private List<String> stringList(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(value, STRING_LIST);
            return values == null ? List.of() : List.copyOf(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to read metric allowed dimensions", e);
        }
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String defaultActor(String value) {
        return hasText(value) ? value.trim() : "system";
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    public record MetricChangeCommand(
            String datasetKey,
            String metricKey,
            String proposedExpression,
            List<String> proposedAllowedDimensions,
            String reason) {
        public MetricChangeCommand {
            proposedAllowedDimensions = proposedAllowedDimensions == null
                    ? List.of()
                    : List.copyOf(proposedAllowedDimensions);
        }
    }

    public record MetricSnapshot(
            String metricKey,
            String expression,
            String valueType,
            List<String> allowedDimensions) {
        public MetricSnapshot {
            allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
        }
    }

    public record ImpactSummary(
            int fieldDependencyCount,
            int lineageNodeCount,
            int lineageEdgeCount,
            int transitiveLineageNodeCount,
            int transitiveLineageEdgeCount,
            int transitivePathCount,
            int transitiveDownstreamNodeCount,
            boolean transitiveTruncated,
            int chartCount,
            int dashboardCount,
            List<String> warnings) {
        public ImpactSummary(int fieldDependencyCount,
                             int lineageNodeCount,
                             int lineageEdgeCount,
                             int chartCount,
                             int dashboardCount,
                             List<String> warnings) {
            this(fieldDependencyCount, lineageNodeCount, lineageEdgeCount, 0, 0, 0, 0,
                    false, chartCount, dashboardCount, warnings);
        }

        public ImpactSummary {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public record MetricChangeReviewView(
            Long id,
            Long tenantId,
            String datasetKey,
            String metricKey,
            MetricSnapshot currentMetric,
            MetricSnapshot proposedMetric,
            ImpactSummary impact,
            String riskLevel,
            String status,
            String requestedBy,
            String requestReason,
            String reviewedBy,
            LocalDateTime reviewedAt,
            String reviewNote,
            LocalDateTime appliedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
