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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
/**
 * CdpWarehouseMetricChangeReviewService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    @Autowired
    /**
     * 初始化 CdpWarehouseMetricChangeReviewService 实例。
     *
     * @param reviewMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param metricLineageService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 CdpWarehouseMetricChangeReviewService 实例。
     *
     * @param reviewMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param metricLineageService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(datasetKey)) {
            query.eq(CdpWarehouseMetricChangeReviewDO::getDatasetKey, datasetKey.trim());
        }
        if (hasText(metricKey)) {
            query.eq(CdpWarehouseMetricChangeReviewDO::getMetricKey, metricKey.trim());
        }
        if (hasText(status)) {
            query.eq(CdpWarehouseMetricChangeReviewDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(reviewMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestedBy requested by 参数，用于 requestChange 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 requestChange 流程生成的业务结果。
     */
    public MetricChangeReviewView requestChange(Long tenantId,
                                                String requestedBy,
                                                MetricChangeCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        reviewMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param reviewId 业务对象 ID，用于定位具体记录。
     * @param reviewer reviewer 参数，用于 approve 流程中的校验、计算或对象转换。
     * @param note note 参数，用于 approve 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param reviewId 业务对象 ID，用于定位具体记录。
     * @param reviewer reviewer 参数，用于 reject 流程中的校验、计算或对象转换。
     * @param note note 参数，用于 reject 流程中的校验、计算或对象转换。
     * @return 返回 reject 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param reviewId 业务对象 ID，用于定位具体记录。
     * @return 返回 apply 流程生成的业务结果。
     */
    public MetricChangeReviewView apply(Long tenantId, Long reviewId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CdpWarehouseMetricChangeReviewDO row = reviewForUpdate(scopedTenantId, reviewId);
        requireStatus(row, STATUS_APPROVED, "only approved metric changes can be applied");

        MetricSnapshot currentSnapshot = metricSnapshot(row.getCurrentSnapshotJson());
        MetricSnapshot proposedSnapshot = metricSnapshot(row.getProposedSnapshotJson());
        BiDatasetDO dataset = persistedDataset(scopedTenantId, row.getDatasetKey());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param dataset dataset 参数，用于 validateAllowedDimensions 流程中的校验、计算或对象转换。
     * @param proposedAllowedDimensions proposed allowed dimensions 参数，用于 validateAllowedDimensions 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private List<String> validateAllowedDimensions(BiDatasetSpec dataset, List<String> proposedAllowedDimensions) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (proposedAllowedDimensions == null || proposedAllowedDimensions.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(normalized);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param reviewId 业务对象 ID，用于定位具体记录。
     * @return 返回 reviewForUpdate 流程生成的业务结果。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 persistedDataset 流程生成的业务结果。
     */
    private BiDatasetDO persistedDataset(Long tenantId, String datasetKey) {
        return datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, tenantId)
                .eq(BiDatasetDO::getDatasetKey, datasetKey)
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .last("LIMIT 1"));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @return 返回 persistedMetric 流程生成的业务结果。
     */
    private BiMetricDO persistedMetric(Long tenantId, Long datasetId, String metricKey) {
        return metricMapper.selectOne(new LambdaQueryWrapper<BiMetricDO>()
                .eq(BiMetricDO::getTenantId, tenantId)
                .eq(BiMetricDO::getDatasetId, datasetId)
                .eq(BiMetricDO::getMetricKey, metricKey)
                .ne(BiMetricDO::getStatus, STATUS_ARCHIVED)
                .last("LIMIT 1"));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param currentSnapshot current snapshot 参数，用于 ensureCurrentStillMatches 流程中的校验、计算或对象转换。
     * @param metric metric 参数，用于 ensureCurrentStillMatches 流程中的校验、计算或对象转换。
     */
    private void ensureCurrentStillMatches(MetricSnapshot currentSnapshot, BiMetricDO metric) {
        if (!currentSnapshot.expression().equals(metric.getExpression())
                || !currentSnapshot.allowedDimensions().equals(stringList(metric.getAllowedDimensionsJson()))) {
            throw new IllegalStateException("metric changed since review was requested: " + metric.getMetricKey());
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param expectedStatus 业务状态，用于筛选或推进状态流转。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    private void requireStatus(CdpWarehouseMetricChangeReviewDO row, String expectedStatus, String message) {
        if (!expectedStatus.equals(row.getStatus())) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param impact impact 参数，用于 toImpactSummary 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param impactSummary impact summary 参数，用于 riskLevel 流程中的校验、计算或对象转换。
     * @return 返回 risk level 生成的文本或业务键。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param transitiveLineage transitive lineage 参数，用于 downstreamNodeCount 流程中的校验、计算或对象转换。
     * @return 返回 downstream node count 计算得到的数量、金额或指标值。
     */
    private int downstreamNodeCount(CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage) {
        return (int) transitiveLineage.nodes().stream()
                .filter(node -> node.relation() == CdpWarehouseCatalogService.LineageRelation.DOWNSTREAM
                        || node.relation() == CdpWarehouseCatalogService.LineageRelation.BOTH)
                .count();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MetricChangeReviewView toView(CdpWarehouseMetricChangeReviewDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 metricSnapshot 流程生成的业务结果。
     */
    private MetricSnapshot metricSnapshot(String value) {
        return fromJson(value, MetricSnapshot.class);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 impactSummary 流程生成的业务结果。
     */
    private ImpactSummary impactSummary(String value) {
        return fromJson(value, ImpactSummary.class);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize metric change review payload", e);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param type 类型标识，用于选择对应处理分支。
     * @return 返回组装或转换后的结果对象。
     */
    private <T> T fromJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to read metric change review payload", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string list 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default actor 生成的文本或业务键。
     */
    private String defaultActor(String value) {
        return hasText(value) ? value.trim() : "system";
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * MetricChangeCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * MetricSnapshot 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record MetricSnapshot(
            String metricKey,
            String expression,
            String valueType,
            List<String> allowedDimensions) {
        public MetricSnapshot {
            allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
        }
    }

    /**
     * ImpactSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
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
        /**
         * 初始化 ImpactSummary 实例。
         *
         * @param fieldDependencyCount field dependency count 参数，用于 ImpactSummary 流程中的校验、计算或对象转换。
         * @param lineageNodeCount lineage node count 参数，用于 ImpactSummary 流程中的校验、计算或对象转换。
         * @param lineageEdgeCount lineage edge count 参数，用于 ImpactSummary 流程中的校验、计算或对象转换。
         * @param chartCount chart count 参数，用于 ImpactSummary 流程中的校验、计算或对象转换。
         * @param dashboardCount dashboard count 参数，用于 ImpactSummary 流程中的校验、计算或对象转换。
         * @param warnings warnings 参数，用于 ImpactSummary 流程中的校验、计算或对象转换。
         */
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

    /**
     * MetricChangeReviewView 承载对应领域的业务规则、流程编排和结果转换。
     */
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
