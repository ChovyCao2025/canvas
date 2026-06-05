package org.chovy.canvas.domain.bi.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiColumnPermissionDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiResourcePermissionDO;
import org.chovy.canvas.dal.dataobject.BiRowPermissionDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiColumnPermissionMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourcePermissionMapper;
import org.chovy.canvas.dal.mapper.BiRowPermissionMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BiPermissionAdminService {

    private static final String RESOURCE_DATASET = "DATASET";
    private static final String RESOURCE_DASHBOARD = "DASHBOARD";
    private static final String RESOURCE_CHART = "CHART";
    private static final String RESOURCE_PORTAL = "PORTAL";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Set<String> RESOURCE_TYPES = Set.of(
            RESOURCE_DATASET,
            RESOURCE_DASHBOARD,
            RESOURCE_CHART,
            RESOURCE_PORTAL,
            "SELF_SERVICE",
            "SPREADSHEET",
            "BIG_SCREEN");
    private static final Set<String> SUBJECT_TYPES = Set.of("USER", "ROLE", "ALL", "USER_GROUP", "GROUP");
    private static final Set<String> ACTION_KEYS = Set.of(
            "VIEW",
            "USE",
            "QUERY",
            "EDIT",
            "PUBLISH",
            "EXPORT",
            "EMBED",
            "SUBSCRIBE",
            "ALL",
            "*");
    private static final Set<String> EFFECTS = Set.of("ALLOW", "DENY");
    private static final Set<String> COLUMN_POLICIES = Set.of("ALLOW", "DENY", "MASK");

    private final BiDatasetMapper datasetMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalMapper portalMapper;
    private final BiResourcePermissionMapper resourcePermissionMapper;
    private final BiRowPermissionMapper rowPermissionMapper;
    private final BiColumnPermissionMapper columnPermissionMapper;
    private final ObjectMapper objectMapper;

    public BiPermissionAdminService(BiDatasetMapper datasetMapper,
                                    BiDashboardMapper dashboardMapper,
                                    BiChartMapper chartMapper,
                                    BiPortalMapper portalMapper,
                                    BiResourcePermissionMapper resourcePermissionMapper,
                                    BiRowPermissionMapper rowPermissionMapper,
                                    BiColumnPermissionMapper columnPermissionMapper,
                                    ObjectMapper objectMapper) {
        this.datasetMapper = datasetMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.resourcePermissionMapper = resourcePermissionMapper;
        this.rowPermissionMapper = rowPermissionMapper;
        this.columnPermissionMapper = columnPermissionMapper;
        this.objectMapper = objectMapper;
    }

    public List<BiResourcePermissionView> listResourcePermissions(Long tenantId,
                                                                  String resourceType,
                                                                  String resourceKey,
                                                                  Long resourceId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        ResourceRef ref = hasText(resourceType) && (hasText(resourceKey) || resourceId != null)
                ? resolveResource(scopedTenantId, resourceType, resourceKey, resourceId)
                : null;
        LambdaQueryWrapper<BiResourcePermissionDO> query =
                new LambdaQueryWrapper<BiResourcePermissionDO>()
                        .eq(BiResourcePermissionDO::getTenantId, scopedTenantId)
                        .orderByAsc(BiResourcePermissionDO::getResourceType)
                        .orderByAsc(BiResourcePermissionDO::getResourceId)
                        .orderByAsc(BiResourcePermissionDO::getSubjectType)
                        .orderByAsc(BiResourcePermissionDO::getSubjectId)
                        .orderByAsc(BiResourcePermissionDO::getActionKey);
        if (ref != null) {
            query.eq(BiResourcePermissionDO::getResourceType, ref.resourceType())
                    .eq(BiResourcePermissionDO::getResourceId, ref.resourceId());
        } else if (hasText(resourceType)) {
            query.eq(BiResourcePermissionDO::getResourceType, upperRequired(resourceType, "resourceType"));
        }
        return safeList(resourcePermissionMapper.selectList(query)).stream()
                .map(this::toResourceView)
                .toList();
    }

    public BiResourcePermissionView upsertResourcePermission(Long tenantId,
                                                             String username,
                                                             BiResourcePermissionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("resource permission command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ResourceRef ref = resolveResource(scopedTenantId,
                command.resourceType(),
                command.resourceKey(),
                command.resourceId());
        String subjectType = enumValue(command.subjectType(), SUBJECT_TYPES, "subjectType");
        String subjectId = required(command.subjectId(), "subjectId");
        String actionKey = enumValue(command.actionKey(), ACTION_KEYS, "actionKey");
        String effect = enumValue(command.effect(), EFFECTS, "effect");
        BiResourcePermissionDO row = resourcePermissionMapper.selectOne(
                new LambdaQueryWrapper<BiResourcePermissionDO>()
                        .eq(BiResourcePermissionDO::getTenantId, scopedTenantId)
                        .eq(BiResourcePermissionDO::getResourceType, ref.resourceType())
                        .eq(BiResourcePermissionDO::getResourceId, ref.resourceId())
                        .eq(BiResourcePermissionDO::getSubjectType, subjectType)
                        .eq(BiResourcePermissionDO::getSubjectId, subjectId)
                        .eq(BiResourcePermissionDO::getActionKey, actionKey)
                        .last("LIMIT 1"));
        if (row == null) {
            row = new BiResourcePermissionDO();
            row.setTenantId(scopedTenantId);
            row.setWorkspaceId(ref.workspaceId());
            row.setResourceType(ref.resourceType());
            row.setResourceId(ref.resourceId());
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setActionKey(actionKey);
            row.setEffect(effect);
            row.setCreatedBy(defaultUser(username));
            resourcePermissionMapper.insert(row);
            row = resourcePermissionMapper.selectOne(new LambdaQueryWrapper<BiResourcePermissionDO>()
                    .eq(BiResourcePermissionDO::getTenantId, scopedTenantId)
                    .eq(BiResourcePermissionDO::getResourceType, ref.resourceType())
                    .eq(BiResourcePermissionDO::getResourceId, ref.resourceId())
                    .eq(BiResourcePermissionDO::getSubjectType, subjectType)
                    .eq(BiResourcePermissionDO::getSubjectId, subjectId)
                    .eq(BiResourcePermissionDO::getActionKey, actionKey)
                    .last("LIMIT 1"));
        } else {
            row.setEffect(effect);
            row.setCreatedBy(defaultUser(username));
            resourcePermissionMapper.updateById(row);
        }
        return toResourceView(row);
    }

    public void deleteResourcePermission(Long tenantId, Long id) {
        deleteOwned(resourcePermissionMapper.selectById(id), normalizeTenant(tenantId), id,
                () -> resourcePermissionMapper.deleteById(id));
    }

    public List<BiRowPermissionView> listRowPermissions(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long datasetId = hasText(datasetKey) ? resolveDataset(scopedTenantId, datasetKey).getId() : null;
        LambdaQueryWrapper<BiRowPermissionDO> query =
                new LambdaQueryWrapper<BiRowPermissionDO>()
                        .eq(BiRowPermissionDO::getTenantId, scopedTenantId)
                        .orderByAsc(BiRowPermissionDO::getDatasetId)
                        .orderByAsc(BiRowPermissionDO::getRuleKey);
        if (datasetId != null) {
            query.eq(BiRowPermissionDO::getDatasetId, datasetId);
        }
        Map<Long, String> datasetKeys = datasetKeys(scopedTenantId);
        return safeList(rowPermissionMapper.selectList(query)).stream()
                .map(row -> toRowView(row, datasetKeys.get(row.getDatasetId())))
                .toList();
    }

    public BiRowPermissionView upsertRowPermission(Long tenantId,
                                                   BiRowPermissionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("row permission command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiDatasetDO dataset = resolveDataset(scopedTenantId, command.datasetKey());
        String ruleKey = required(command.ruleKey(), "ruleKey");
        String subjectType = enumValue(command.subjectType(), SUBJECT_TYPES, "subjectType");
        String subjectId = required(command.subjectId(), "subjectId");
        String filterJson = rowFilterJson(command);
        Boolean enabled = command.enabled() == null ? true : command.enabled();
        BiRowPermissionDO row = rowPermissionMapper.selectOne(new LambdaQueryWrapper<BiRowPermissionDO>()
                .eq(BiRowPermissionDO::getTenantId, scopedTenantId)
                .eq(BiRowPermissionDO::getDatasetId, dataset.getId())
                .eq(BiRowPermissionDO::getRuleKey, ruleKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new BiRowPermissionDO();
            row.setTenantId(scopedTenantId);
            row.setDatasetId(dataset.getId());
            row.setRuleKey(ruleKey);
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setFilterJson(filterJson);
            row.setEnabled(enabled);
            rowPermissionMapper.insert(row);
            row = rowPermissionMapper.selectOne(new LambdaQueryWrapper<BiRowPermissionDO>()
                    .eq(BiRowPermissionDO::getTenantId, scopedTenantId)
                    .eq(BiRowPermissionDO::getDatasetId, dataset.getId())
                    .eq(BiRowPermissionDO::getRuleKey, ruleKey)
                    .last("LIMIT 1"));
        } else {
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setFilterJson(filterJson);
            row.setEnabled(enabled);
            rowPermissionMapper.updateById(row);
        }
        return toRowView(row, dataset.getDatasetKey());
    }

    public void deleteRowPermission(Long tenantId, Long id) {
        deleteOwned(rowPermissionMapper.selectById(id), normalizeTenant(tenantId), id,
                () -> rowPermissionMapper.deleteById(id));
    }

    public List<BiColumnPermissionView> listColumnPermissions(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long datasetId = hasText(datasetKey) ? resolveDataset(scopedTenantId, datasetKey).getId() : null;
        LambdaQueryWrapper<BiColumnPermissionDO> query =
                new LambdaQueryWrapper<BiColumnPermissionDO>()
                        .eq(BiColumnPermissionDO::getTenantId, scopedTenantId)
                        .orderByAsc(BiColumnPermissionDO::getDatasetId)
                        .orderByAsc(BiColumnPermissionDO::getFieldKey)
                        .orderByAsc(BiColumnPermissionDO::getSubjectType)
                        .orderByAsc(BiColumnPermissionDO::getSubjectId);
        if (datasetId != null) {
            query.eq(BiColumnPermissionDO::getDatasetId, datasetId);
        }
        Map<Long, String> datasetKeys = datasetKeys(scopedTenantId);
        return safeList(columnPermissionMapper.selectList(query)).stream()
                .map(row -> toColumnView(row, datasetKeys.get(row.getDatasetId())))
                .toList();
    }

    public BiColumnPermissionView upsertColumnPermission(Long tenantId,
                                                         BiColumnPermissionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("column permission command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiDatasetDO dataset = resolveDataset(scopedTenantId, command.datasetKey());
        String fieldKey = required(command.fieldKey(), "fieldKey");
        String subjectType = enumValue(command.subjectType(), SUBJECT_TYPES, "subjectType");
        String subjectId = required(command.subjectId(), "subjectId");
        String policy = enumValue(command.policy(), COLUMN_POLICIES, "policy");
        String maskJson = command.mask().isEmpty() ? null : json(command.mask());
        Boolean enabled = command.enabled() == null ? true : command.enabled();
        BiColumnPermissionDO row = columnPermissionMapper.selectOne(new LambdaQueryWrapper<BiColumnPermissionDO>()
                .eq(BiColumnPermissionDO::getTenantId, scopedTenantId)
                .eq(BiColumnPermissionDO::getDatasetId, dataset.getId())
                .eq(BiColumnPermissionDO::getFieldKey, fieldKey)
                .eq(BiColumnPermissionDO::getSubjectType, subjectType)
                .eq(BiColumnPermissionDO::getSubjectId, subjectId)
                .last("LIMIT 1"));
        if (row == null) {
            row = new BiColumnPermissionDO();
            row.setTenantId(scopedTenantId);
            row.setDatasetId(dataset.getId());
            row.setFieldKey(fieldKey);
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setPolicy(policy);
            row.setMaskJson(maskJson);
            row.setEnabled(enabled);
            columnPermissionMapper.insert(row);
            row = columnPermissionMapper.selectOne(new LambdaQueryWrapper<BiColumnPermissionDO>()
                    .eq(BiColumnPermissionDO::getTenantId, scopedTenantId)
                    .eq(BiColumnPermissionDO::getDatasetId, dataset.getId())
                    .eq(BiColumnPermissionDO::getFieldKey, fieldKey)
                    .eq(BiColumnPermissionDO::getSubjectType, subjectType)
                    .eq(BiColumnPermissionDO::getSubjectId, subjectId)
                    .last("LIMIT 1"));
        } else {
            row.setPolicy(policy);
            row.setMaskJson(maskJson);
            row.setEnabled(enabled);
            columnPermissionMapper.updateById(row);
        }
        return toColumnView(row, dataset.getDatasetKey());
    }

    public void deleteColumnPermission(Long tenantId, Long id) {
        deleteOwned(columnPermissionMapper.selectById(id), normalizeTenant(tenantId), id,
                () -> columnPermissionMapper.deleteById(id));
    }

    private ResourceRef resolveResource(Long tenantId,
                                        String resourceType,
                                        String resourceKey,
                                        Long resourceId) {
        String scopedType = enumValue(resourceType, RESOURCE_TYPES, "resourceType");
        if (resourceId != null && resourceId > 0) {
            return new ResourceRef(scopedType, resourceId, workspaceId(tenantId, scopedType, resourceId), resourceKey);
        }
        if (!hasText(resourceKey)) {
            throw new IllegalArgumentException("resourceKey or resourceId is required");
        }
        if (RESOURCE_DATASET.equals(scopedType)) {
            BiDatasetDO dataset = resolveDataset(tenantId, resourceKey);
            return new ResourceRef(scopedType, dataset.getId(), dataset.getWorkspaceId(), dataset.getDatasetKey());
        }
        if (RESOURCE_DASHBOARD.equals(scopedType)) {
            BiDashboardDO dashboard = dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                    .in(BiDashboardDO::getTenantId, tenantScope(tenantId))
                    .eq(BiDashboardDO::getDashboardKey, required(resourceKey, "resourceKey"))
                    .ne(BiDashboardDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiDashboardDO::getTenantId)
                    .last("LIMIT 1"));
            if (dashboard == null || dashboard.getId() == null) {
                throw new IllegalArgumentException("BI dashboard not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, dashboard.getId(), dashboard.getWorkspaceId(), dashboard.getDashboardKey());
        }
        if (RESOURCE_CHART.equals(scopedType)) {
            BiChartDO chart = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                    .in(BiChartDO::getTenantId, tenantScope(tenantId))
                    .eq(BiChartDO::getChartKey, required(resourceKey, "resourceKey"))
                    .ne(BiChartDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiChartDO::getTenantId)
                    .last("LIMIT 1"));
            if (chart == null || chart.getId() == null) {
                throw new IllegalArgumentException("BI chart not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, chart.getId(), chart.getWorkspaceId(), chart.getChartKey());
        }
        if (RESOURCE_PORTAL.equals(scopedType)) {
            BiPortalDO portal = portalMapper.selectOne(new LambdaQueryWrapper<BiPortalDO>()
                    .in(BiPortalDO::getTenantId, tenantScope(tenantId))
                    .eq(BiPortalDO::getPortalKey, required(resourceKey, "resourceKey"))
                    .ne(BiPortalDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiPortalDO::getTenantId)
                    .last("LIMIT 1"));
            if (portal == null || portal.getId() == null) {
                throw new IllegalArgumentException("BI portal not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, portal.getId(), portal.getWorkspaceId(), portal.getPortalKey());
        }
        throw new IllegalArgumentException("resourceId is required for resourceType: " + scopedType);
    }

    private Long workspaceId(Long tenantId, String resourceType, Long resourceId) {
        if (RESOURCE_DATASET.equals(resourceType)) {
            BiDatasetDO row = datasetMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_DASHBOARD.equals(resourceType)) {
            BiDashboardDO row = dashboardMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_CHART.equals(resourceType)) {
            BiChartDO row = chartMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_PORTAL.equals(resourceType)) {
            BiPortalDO row = portalMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        return 0L;
    }

    private BiDatasetDO resolveDataset(Long tenantId, String datasetKey) {
        BiDatasetDO dataset = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiDatasetDO::getTenantId)
                .last("LIMIT 1"));
        if (dataset == null || dataset.getId() == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        return dataset;
    }

    private BiResourcePermissionView toResourceView(BiResourcePermissionDO row) {
        if (row == null) {
            throw new IllegalStateException("BI resource permission was not persisted");
        }
        return new BiResourcePermissionView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                resourceKey(row.getResourceType(), row.getResourceId()),
                row.getResourceId(),
                row.getSubjectType(),
                row.getSubjectId(),
                row.getActionKey(),
                row.getEffect(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    private BiRowPermissionView toRowView(BiRowPermissionDO row, String datasetKey) {
        if (row == null) {
            throw new IllegalStateException("BI row permission was not persisted");
        }
        return new BiRowPermissionView(
                row.getId(),
                row.getTenantId(),
                datasetKey,
                row.getDatasetId(),
                row.getRuleKey(),
                row.getSubjectType(),
                row.getSubjectId(),
                row.getFilterJson(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getCreatedAt());
    }

    private BiColumnPermissionView toColumnView(BiColumnPermissionDO row, String datasetKey) {
        if (row == null) {
            throw new IllegalStateException("BI column permission was not persisted");
        }
        return new BiColumnPermissionView(
                row.getId(),
                row.getTenantId(),
                datasetKey,
                row.getDatasetId(),
                row.getFieldKey(),
                row.getSubjectType(),
                row.getSubjectId(),
                row.getPolicy(),
                row.getMaskJson(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getCreatedAt());
    }

    private Map<Long, String> datasetKeys(Long tenantId) {
        Map<Long, String> values = new LinkedHashMap<>();
        for (BiDatasetDO row : safeList(datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByAsc(BiDatasetDO::getDatasetKey)))) {
            values.put(row.getId(), row.getDatasetKey());
        }
        return values;
    }

    private String resourceKey(String resourceType, Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        if (RESOURCE_DATASET.equals(resourceType)) {
            BiDatasetDO row = datasetMapper.selectById(resourceId);
            return row == null ? null : row.getDatasetKey();
        }
        if (RESOURCE_DASHBOARD.equals(resourceType)) {
            BiDashboardDO row = dashboardMapper.selectById(resourceId);
            return row == null ? null : row.getDashboardKey();
        }
        if (RESOURCE_CHART.equals(resourceType)) {
            BiChartDO row = chartMapper.selectById(resourceId);
            return row == null ? null : row.getChartKey();
        }
        if (RESOURCE_PORTAL.equals(resourceType)) {
            BiPortalDO row = portalMapper.selectById(resourceId);
            return row == null ? null : row.getPortalKey();
        }
        return null;
    }

    private String rowFilterJson(BiRowPermissionCommand command) {
        if (!command.filters().isEmpty()) {
            return json(Map.of("filters", command.filters()));
        }
        if (!command.filter().isEmpty()) {
            return json(command.filter());
        }
        throw new IllegalArgumentException("row permission filter is required");
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI permission payload", e);
        }
    }

    private void deleteOwned(Object row, Long tenantId, Long id, Runnable deleteAction) {
        if (row == null) {
            throw new IllegalArgumentException("BI permission not found: " + id);
        }
        Long rowTenantId;
        if (row instanceof BiResourcePermissionDO value) {
            rowTenantId = value.getTenantId();
        } else if (row instanceof BiRowPermissionDO value) {
            rowTenantId = value.getTenantId();
        } else if (row instanceof BiColumnPermissionDO value) {
            rowTenantId = value.getTenantId();
        } else {
            throw new IllegalArgumentException("unsupported BI permission row: " + id);
        }
        if (!normalizeTenant(rowTenantId).equals(tenantId)) {
            throw new IllegalArgumentException("BI permission does not belong to current tenant: " + id);
        }
        deleteAction.run();
    }

    private String enumValue(String value, Set<String> allowed, String fieldName) {
        String normalized = upperRequired(value, fieldName);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("unsupported " + fieldName + ": " + value);
        }
        return normalized;
    }

    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, scopedTenantId);
    }

    private String defaultUser(String username) {
        return hasText(username) ? username.trim() : "system";
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
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

    private record ResourceRef(
            String resourceType,
            Long resourceId,
            Long workspaceId,
            String resourceKey) {
    }
}
