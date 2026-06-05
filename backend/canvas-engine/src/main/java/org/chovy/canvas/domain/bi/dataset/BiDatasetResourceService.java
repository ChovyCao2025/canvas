package org.chovy.canvas.domain.bi.dataset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiDatasetFieldDO;
import org.chovy.canvas.dal.dataobject.BiDatasetVersionDO;
import org.chovy.canvas.dal.dataobject.BiMetricDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDatasetFieldMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiDatasetVersionMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistry;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class BiDatasetResourceService implements BiDatasetSpecResolver {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Pattern TABLE_EXPRESSION = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+){0,2}");
    private static final Pattern COLUMN_EXPRESSION = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)?");
    private static final Pattern METRIC_EXPRESSION = Pattern.compile("[A-Za-z0-9_\\s().,+\\-*/<>=]+");
    private static final Set<String> DATASET_TYPES = Set.of("TABLE", "VIEW");
    private static final Set<String> FIELD_ROLES = Set.of("DIMENSION", "MEASURE");
    private static final Set<String> DATA_TYPES = Set.of("STRING", "NUMBER", "DATE", "DATETIME", "BOOLEAN", "PERCENT");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDatasetFieldMapper fieldMapper;
    private final BiMetricMapper metricMapper;
    private final BiDatasetVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final BiResourcePermissionGuard permissionGuard;
    private final BiPublishApprovalService publishApprovalService;
    private final BiResourceCollaborationService collaborationService;

    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    ObjectMapper objectMapper) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, null, objectMapper);
    }

    @Autowired
    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    ObjectProvider<BiResourcePermissionGuard> permissionGuardProvider,
                                    ObjectProvider<BiPublishApprovalService> publishApprovalServiceProvider,
                                    ObjectProvider<BiResourceCollaborationService> collaborationServiceProvider) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                permissionGuardProvider == null ? null : permissionGuardProvider.getIfAvailable(),
                publishApprovalServiceProvider == null ? null : publishApprovalServiceProvider.getIfAvailable(),
                collaborationServiceProvider == null ? null : collaborationServiceProvider.getIfAvailable());
    }

    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                (BiResourcePermissionGuard) null);
    }

    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    BiResourcePermissionGuard permissionGuard) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                permissionGuard, null, null);
    }

    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    BiResourcePermissionGuard permissionGuard,
                                    BiPublishApprovalService publishApprovalService) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                permissionGuard, publishApprovalService, null);
    }

    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    BiResourcePermissionGuard permissionGuard,
                                    BiPublishApprovalService publishApprovalService,
                                    BiResourceCollaborationService collaborationService) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.fieldMapper = fieldMapper;
        this.metricMapper = metricMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
        this.permissionGuard = permissionGuard;
        this.publishApprovalService = publishApprovalService;
        this.collaborationService = collaborationService;
    }

    public List<BiDatasetResource> listResources(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                        .eq(BiDatasetDO::getTenantId, scopedTenantId)
                        .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                        .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiDatasetDO::getUpdatedAt)
                        .orderByAsc(BiDatasetDO::getDatasetKey)))
                .stream()
                .map(row -> toResource(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId())))
                .toList();
    }

    public BiDatasetResource getResource(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            row = find(0L, workspaceId(0L), datasetKey);
        }
        if (row == null) {
            BiDatasetSpec builtIn = MarketingBiDatasetRegistry.dataset(datasetKey);
            return fromBuiltIn(builtIn);
        }
        return toResource(row, fields(row.getTenantId(), row.getId()), metrics(row.getTenantId(), row.getId()));
    }

    public BiDatasetResource saveDraft(Long tenantId, String username, BiDatasetResource resource) {
        return saveDraft(tenantId, username, null, resource);
    }

    public BiDatasetResource saveDraft(Long tenantId, String username, String role, BiDatasetResource resource) {
        return saveDraftInternal(tenantId, username, role, resource, null, false);
    }

    public BiDatasetResource saveDraft(Long tenantId,
                                       String username,
                                       String role,
                                       BiDatasetResource resource,
                                       String lockToken) {
        return saveDraftInternal(tenantId, username, role, resource, lockToken, true);
    }

    private BiDatasetResource saveDraftInternal(Long tenantId,
                                                String username,
                                                String role,
                                                BiDatasetResource resource,
                                                String lockToken,
                                                boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        validateResource(resource);
        BiDatasetDO existing = find(scopedTenantId, workspaceId, resource.datasetKey());
        requirePermission(scopedTenantId, workspaceId, "DATASET", existing == null ? null : existing.getId(),
                username, role, BiPermissionService.ACTION_EDIT);
        requireEditLock(scopedTenantId, workspaceId, "DATASET", resource.datasetKey(), username, role,
                lockToken, enforceEditLock && existing != null);

        BiDatasetDO row = new BiDatasetDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetKey(required(resource.datasetKey(), "datasetKey"));
        row.setName(required(resource.name(), "name"));
        row.setDatasetType(required(resource.datasetType(), "datasetType"));
        row.setTableExpression(required(resource.tableExpression(), "tableExpression"));
        row.setTenantColumn(required(resource.tenantColumn(), "tenantColumn"));
        row.setModelJson(json(resource.model()));
        row.setStatus(STATUS_DRAFT);
        row.setCreatedBy(username == null || username.isBlank() ? "system" : username);
        datasetMapper.upsert(row);

        BiDatasetDO persisted = find(scopedTenantId, workspaceId, resource.datasetKey());
        Long datasetId = persisted == null ? row.getId() : persisted.getId();
        if (datasetId == null) {
            throw new IllegalStateException("BI dataset was not persisted: " + resource.datasetKey());
        }
        fieldMapper.deleteByDataset(scopedTenantId, datasetId);
        metricMapper.deleteByDataset(scopedTenantId, datasetId);
        for (BiDatasetFieldResource field : resource.fields()) {
            fieldMapper.insert(toField(scopedTenantId, datasetId, field));
        }
        for (BiMetricResource metric : resource.metrics()) {
            metricMapper.insert(toMetric(scopedTenantId, workspaceId, datasetId, metric));
        }
        return toResource(persisted == null ? row : persisted,
                resource.fields(),
                resource.metrics());
    }

    public BiDatasetResource publish(Long tenantId, String datasetKey) {
        return publish(tenantId, null, datasetKey);
    }

    public BiDatasetResource publish(Long tenantId, String username, String datasetKey) {
        return publish(tenantId, username, null, datasetKey);
    }

    public BiDatasetResource publish(Long tenantId, String username, String role, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        requirePermission(scopedTenantId, workspaceId, "DATASET", row.getId(), username, role, BiPermissionService.ACTION_PUBLISH);
        requirePublishApproval(scopedTenantId, workspaceId, "DATASET", datasetKey, row.getUpdatedAt(), role);
        datasetMapper.publish(scopedTenantId, workspaceId, datasetKey);
        BiDatasetDO published = find(scopedTenantId, workspaceId, datasetKey);
        if (published == null) {
            row.setStatus(STATUS_PUBLISHED);
            BiDatasetResource resource = toResource(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId()));
            insertVersionSnapshot(scopedTenantId, workspaceId, row, resource, username);
            return resource;
        }
        BiDatasetResource resource =
                toResource(published, fields(scopedTenantId, published.getId()), metrics(scopedTenantId, published.getId()));
        insertVersionSnapshot(scopedTenantId, workspaceId, published, resource, username);
        return resource;
    }

    public BiDatasetResource archive(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        datasetMapper.archive(scopedTenantId, workspaceId, datasetKey);
        BiDatasetDO archived = find(scopedTenantId, workspaceId, datasetKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return toResource(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId()));
        }
        return toResource(archived, fields(scopedTenantId, archived.getId()), metrics(scopedTenantId, archived.getId()));
    }

    public List<BiDatasetVersionView> listVersions(Long tenantId, String datasetKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiDatasetVersionDO>()
                        .eq(BiDatasetVersionDO::getTenantId, scopedTenantId)
                        .eq(BiDatasetVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiDatasetVersionDO::getDatasetId, row.getId())
                        .orderByDesc(BiDatasetVersionDO::getVersion)
                        .last("LIMIT " + capped)))
                .stream()
                .map(this::toVersionView)
                .toList();
    }

    public BiDatasetResource restoreVersion(Long tenantId, String username, String datasetKey, int version) {
        return restoreVersion(tenantId, username, null, datasetKey, version);
    }

    public BiDatasetResource restoreVersion(Long tenantId, String username, String role, String datasetKey, int version) {
        return restoreVersionInternal(tenantId, username, role, datasetKey, version, null, false);
    }

    public BiDatasetResource restoreVersion(Long tenantId,
                                            String username,
                                            String role,
                                            String datasetKey,
                                            int version,
                                            String lockToken) {
        return restoreVersionInternal(tenantId, username, role, datasetKey, version, lockToken, true);
    }

    private BiDatasetResource restoreVersionInternal(Long tenantId,
                                                     String username,
                                                     String role,
                                                     String datasetKey,
                                                     int version,
                                                     String lockToken,
                                                     boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        if (version <= 0) {
            throw new IllegalArgumentException("dataset version must be positive");
        }
        if (versionMapper == null) {
            throw new IllegalStateException("BI dataset version mapper is required");
        }
        BiDatasetVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiDatasetVersionDO>()
                .eq(BiDatasetVersionDO::getTenantId, scopedTenantId)
                .eq(BiDatasetVersionDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetVersionDO::getDatasetId, row.getId())
                .eq(BiDatasetVersionDO::getVersion, version)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI dataset version not found: " + datasetKey + " v" + version);
        }
        if (enforceEditLock) {
            return saveDraft(scopedTenantId, username, role, resourceFromJson(snapshot.getResourceJson()), lockToken);
        }
        return saveDraft(scopedTenantId, username, role, resourceFromJson(snapshot.getResourceJson()));
    }

    private void requirePermission(Long tenantId,
                                   Long workspaceId,
                                   String resourceType,
                                   Long resourceId,
                                   String username,
                                   String role,
                                   String actionKey) {
        if (permissionGuard != null && resourceId != null) {
            permissionGuard.require(tenantId, workspaceId, resourceType, resourceId, username, role, actionKey);
        }
    }

    private void requirePublishApproval(Long tenantId,
                                        Long workspaceId,
                                        String resourceType,
                                        String resourceKey,
                                        java.time.LocalDateTime resourceUpdatedAt,
                                        String role) {
        if (publishApprovalService != null && !canBypassPublishApproval(role)) {
            publishApprovalService.requireApprovedApproval(
                    tenantId, workspaceId, resourceType, resourceKey, resourceUpdatedAt);
        }
    }

    private void requireEditLock(Long tenantId,
                                 Long workspaceId,
                                 String resourceType,
                                 String resourceKey,
                                 String username,
                                 String role,
                                 String lockToken,
                                 boolean required) {
        if (required && collaborationService != null && !canBypassEditLock(role)) {
            collaborationService.requireCurrentLock(tenantId, workspaceId, resourceType, resourceKey, username, lockToken);
        }
    }

    private boolean canBypassEditLock(String role) {
        return canBypassPublishApproval(role);
    }

    private boolean canBypassPublishApproval(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return RoleNames.ADMIN.equals(normalized)
                || RoleNames.SUPER_ADMIN.equals(normalized)
                || RoleNames.TENANT_ADMIN.equals(normalized);
    }

    @Override
    public BiDatasetSpec dataset(String datasetKey, Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            row = find(0L, workspaceId(0L), datasetKey);
        }
        if (row == null || STATUS_ARCHIVED.equals(row.getStatus())) {
            return MarketingBiDatasetRegistry.dataset(datasetKey);
        }
        return toSpec(row, fields(row.getTenantId(), row.getId()), metrics(row.getTenantId(), row.getId()));
    }

    @Override
    public List<BiDatasetSpec> datasets(Long tenantId) {
        Map<String, BiDatasetSpec> result = new LinkedHashMap<>();
        for (BiDatasetSpec builtIn : MarketingBiDatasetRegistry.datasets()) {
            result.put(builtIn.datasetKey(), builtIn);
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        for (BiDatasetDO row : safeList(datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, scopedTenantId)
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)))) {
            result.put(row.getDatasetKey(), toSpec(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId())));
        }
        return List.copyOf(result.values());
    }

    private void validateResource(BiDatasetResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("dataset resource is required");
        }
        if (!RESOURCE_KEY.matcher(required(resource.datasetKey(), "datasetKey")).matches()) {
            throw new IllegalArgumentException("datasetKey contains unsafe characters");
        }
        required(resource.name(), "name");
        String datasetType = required(resource.datasetType(), "datasetType");
        if (!DATASET_TYPES.contains(datasetType)) {
            throw new IllegalArgumentException("unsupported dataset type: " + datasetType);
        }
        if (!TABLE_EXPRESSION.matcher(required(resource.tableExpression(), "tableExpression")).matches()) {
            throw new IllegalArgumentException("tableExpression must be a qualified table name");
        }
        if (!COLUMN_EXPRESSION.matcher(required(resource.tenantColumn(), "tenantColumn")).matches()) {
            throw new IllegalArgumentException("tenantColumn must be a column identifier");
        }
        if (resource.fields().isEmpty()) {
            throw new IllegalArgumentException("dataset fields are required");
        }
        if (resource.metrics().isEmpty()) {
            throw new IllegalArgumentException("dataset metrics are required");
        }
        Set<String> fieldKeys = new LinkedHashSet<>();
        for (BiDatasetFieldResource field : resource.fields()) {
            validateField(field);
            if (!fieldKeys.add(field.fieldKey())) {
                throw new IllegalArgumentException("duplicate dataset field: " + field.fieldKey());
            }
        }
        Set<String> metricKeys = new LinkedHashSet<>();
        for (BiMetricResource metric : resource.metrics()) {
            validateMetric(metric, fieldKeys);
            if (!metricKeys.add(metric.metricKey())) {
                throw new IllegalArgumentException("duplicate metric: " + metric.metricKey());
            }
        }
    }

    private void validateField(BiDatasetFieldResource field) {
        if (field == null) {
            throw new IllegalArgumentException("dataset field is required");
        }
        if (!RESOURCE_KEY.matcher(required(field.fieldKey(), "fieldKey")).matches()) {
            throw new IllegalArgumentException("fieldKey contains unsafe characters");
        }
        required(field.displayName(), "displayName");
        if (!COLUMN_EXPRESSION.matcher(required(field.columnExpression(), "columnExpression")).matches()) {
            throw new IllegalArgumentException("columnExpression must be a column identifier");
        }
        if (!FIELD_ROLES.contains(required(field.role(), "role"))) {
            throw new IllegalArgumentException("unsupported field role: " + field.role());
        }
        if (!DATA_TYPES.contains(required(field.dataType(), "dataType"))) {
            throw new IllegalArgumentException("unsupported field data type: " + field.dataType());
        }
    }

    private void validateMetric(BiMetricResource metric, Set<String> fieldKeys) {
        if (metric == null) {
            throw new IllegalArgumentException("metric is required");
        }
        if (!RESOURCE_KEY.matcher(required(metric.metricKey(), "metricKey")).matches()) {
            throw new IllegalArgumentException("metricKey contains unsafe characters");
        }
        required(metric.displayName(), "displayName");
        String expression = required(metric.expression(), "expression");
        if (!METRIC_EXPRESSION.matcher(expression).matches()
                || expression.contains("--")
                || expression.contains("/*")
                || expression.contains(";")) {
            throw new IllegalArgumentException("metric expression contains unsafe characters");
        }
        required(metric.aggregation(), "aggregation");
        if (!DATA_TYPES.contains(required(metric.dataType(), "dataType"))) {
            throw new IllegalArgumentException("unsupported metric data type: " + metric.dataType());
        }
        for (String dimension : metric.allowedDimensions()) {
            if (!fieldKeys.contains(dimension)) {
                throw new IllegalArgumentException("metric allowed dimension is not a dataset field: " + dimension);
            }
        }
    }

    private BiDatasetDO find(Long tenantId, Long workspaceId, String datasetKey) {
        return datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, tenantId)
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .last("LIMIT 1"));
    }

    private Long workspaceId(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1"));
        return workspace == null || workspace.getId() == null ? 0L : workspace.getId();
    }

    private List<BiDatasetFieldResource> fields(Long tenantId, Long datasetId) {
        return safeList(fieldMapper.selectList(new LambdaQueryWrapper<BiDatasetFieldDO>()
                        .eq(BiDatasetFieldDO::getTenantId, tenantId)
                        .eq(BiDatasetFieldDO::getDatasetId, datasetId)
                        .orderByAsc(BiDatasetFieldDO::getSortOrder)
                        .orderByAsc(BiDatasetFieldDO::getFieldKey)))
                .stream()
                .map(this::toField)
                .toList();
    }

    private List<BiMetricResource> metrics(Long tenantId, Long datasetId) {
        return safeList(metricMapper.selectList(new LambdaQueryWrapper<BiMetricDO>()
                        .eq(BiMetricDO::getTenantId, tenantId)
                        .eq(BiMetricDO::getDatasetId, datasetId)
                        .orderByAsc(BiMetricDO::getMetricKey)))
                .stream()
                .map(this::toMetric)
                .toList();
    }

    private BiDatasetResource toResource(BiDatasetDO row,
                                         List<BiDatasetFieldResource> fields,
                                         List<BiMetricResource> metrics) {
        return new BiDatasetResource(
                row.getDatasetKey(),
                row.getName(),
                row.getDatasetType(),
                row.getTableExpression(),
                row.getTenantColumn(),
                map(row.getModelJson()),
                fields,
                metrics,
                row.getStatus(),
                "PERSISTED");
    }

    private BiDatasetResource fromBuiltIn(BiDatasetSpec dataset) {
        List<BiDatasetFieldResource> fields = dataset.fields().values().stream()
                .map(field -> new BiDatasetFieldResource(
                        field.fieldKey(),
                        field.fieldKey(),
                        field.columnExpression(),
                        field.role().name(),
                        field.valueType(),
                        null,
                        null,
                        null,
                        null,
                        true,
                        "NORMAL",
                        0))
                .toList();
        List<BiMetricResource> metrics = dataset.metrics().values().stream()
                .map(metric -> new BiMetricResource(
                        metric.metricKey(),
                        metric.metricKey(),
                        metric.expression(),
                        "CUSTOM",
                        metric.valueType(),
                        null,
                        null,
                        metric.allowedDimensions(),
                        "system",
                        null,
                        "ACTIVE"))
                .toList();
        return new BiDatasetResource(
                dataset.datasetKey(),
                dataset.datasetKey(),
                "TABLE",
                dataset.tableExpression(),
                dataset.tenantColumn(),
                Map.of("preset", true),
                fields,
                metrics,
                STATUS_PUBLISHED,
                "PRESET");
    }

    private BiDatasetSpec toSpec(BiDatasetDO row,
                                List<BiDatasetFieldResource> fields,
                                List<BiMetricResource> metrics) {
        Map<String, BiFieldSpec> fieldSpecs = new LinkedHashMap<>();
        for (BiDatasetFieldResource field : fields) {
            if (!field.visible()) {
                continue;
            }
            fieldSpecs.put(field.fieldKey(), new BiFieldSpec(
                    field.fieldKey(),
                    field.columnExpression(),
                    BiFieldSpec.Role.valueOf(field.role()),
                    field.dataType()));
        }
        Map<String, BiMetricSpec> metricSpecs = new LinkedHashMap<>();
        for (BiMetricResource metric : metrics) {
            if (STATUS_ARCHIVED.equals(metric.status())) {
                continue;
            }
            metricSpecs.put(metric.metricKey(), new BiMetricSpec(
                    metric.metricKey(),
                    metric.expression(),
                    metric.dataType(),
                    metric.allowedDimensions()));
        }
        return new BiDatasetSpec(row.getDatasetKey(), row.getTableExpression(), row.getTenantColumn(), fieldSpecs, metricSpecs);
    }

    private BiDatasetFieldDO toField(Long tenantId, Long datasetId, BiDatasetFieldResource field) {
        BiDatasetFieldDO row = new BiDatasetFieldDO();
        row.setTenantId(tenantId);
        row.setDatasetId(datasetId);
        row.setFieldKey(required(field.fieldKey(), "fieldKey"));
        row.setDisplayName(required(field.displayName(), "displayName"));
        row.setColumnExpression(required(field.columnExpression(), "columnExpression"));
        row.setRoleKey(required(field.role(), "role"));
        row.setDataType(required(field.dataType(), "dataType"));
        row.setSemanticType(field.semanticType());
        row.setDefaultAggregation(field.defaultAggregation());
        row.setFormatPattern(field.formatPattern());
        row.setUnit(field.unit());
        row.setVisible(field.visible());
        row.setSensitiveLevel(field.sensitiveLevel() == null || field.sensitiveLevel().isBlank() ? "NORMAL" : field.sensitiveLevel());
        row.setSortOrder(field.sortOrder());
        return row;
    }

    private BiMetricDO toMetric(Long tenantId, Long workspaceId, Long datasetId, BiMetricResource metric) {
        BiMetricDO row = new BiMetricDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetId(datasetId);
        row.setMetricKey(required(metric.metricKey(), "metricKey"));
        row.setDisplayName(required(metric.displayName(), "displayName"));
        row.setExpression(required(metric.expression(), "expression"));
        row.setAggregation(required(metric.aggregation(), "aggregation"));
        row.setDataType(required(metric.dataType(), "dataType"));
        row.setUnit(metric.unit());
        row.setFormatPattern(metric.formatPattern());
        row.setAllowedDimensionsJson(json(metric.allowedDimensions()));
        row.setOwner(metric.owner());
        row.setDescription(metric.description());
        row.setStatus(metric.status() == null || metric.status().isBlank() ? "ACTIVE" : metric.status());
        return row;
    }

    private BiDatasetFieldResource toField(BiDatasetFieldDO row) {
        return new BiDatasetFieldResource(
                row.getFieldKey(),
                row.getDisplayName(),
                row.getColumnExpression(),
                row.getRoleKey(),
                row.getDataType(),
                row.getSemanticType(),
                row.getDefaultAggregation(),
                row.getFormatPattern(),
                row.getUnit(),
                Boolean.TRUE.equals(row.getVisible()),
                row.getSensitiveLevel(),
                row.getSortOrder() == null ? 0 : row.getSortOrder());
    }

    private BiMetricResource toMetric(BiMetricDO row) {
        return new BiMetricResource(
                row.getMetricKey(),
                row.getDisplayName(),
                row.getExpression(),
                row.getAggregation(),
                row.getDataType(),
                row.getUnit(),
                row.getFormatPattern(),
                stringList(row.getAllowedDimensionsJson()),
                row.getOwner(),
                row.getDescription(),
                row.getStatus());
    }

    private void insertVersionSnapshot(Long tenantId,
                                       Long workspaceId,
                                       BiDatasetDO dataset,
                                       BiDatasetResource resource,
                                       String username) {
        if (versionMapper == null) {
            return;
        }
        if (dataset.getId() == null) {
            throw new IllegalStateException("BI dataset id is required for version snapshot: " + dataset.getDatasetKey());
        }
        BiDatasetVersionDO row = new BiDatasetVersionDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetId(dataset.getId());
        row.setDatasetKey(dataset.getDatasetKey());
        row.setVersion(nextVersion(tenantId, workspaceId, dataset.getId()));
        row.setStatus(STATUS_PUBLISHED);
        row.setResourceJson(json(resource));
        row.setPublishedBy(defaultUser(username));
        versionMapper.insert(row);
    }

    private int nextVersion(Long tenantId, Long workspaceId, Long datasetId) {
        if (versionMapper == null) {
            return 1;
        }
        BiDatasetVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<BiDatasetVersionDO>()
                .eq(BiDatasetVersionDO::getTenantId, tenantId)
                .eq(BiDatasetVersionDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetVersionDO::getDatasetId, datasetId)
                .orderByDesc(BiDatasetVersionDO::getVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
    }

    private BiDatasetVersionView toVersionView(BiDatasetVersionDO row) {
        return new BiDatasetVersionView(
                row.getId(),
                row.getDatasetKey(),
                row.getVersion(),
                row.getStatus(),
                resourceFromJson(row.getResourceJson()),
                row.getPublishedBy(),
                row.getCreatedAt());
    }

    private BiDatasetResource resourceFromJson(String json) {
        try {
            return objectMapper.readValue(json, BiDatasetResource.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI dataset version payload", e);
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> stringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI dataset payload", e);
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
