package org.chovy.canvas.domain.bi.subscription;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAlertRuleDO;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiMetricDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiAlertRuleMapper;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiSubscriptionMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class BiSubscriptionAdminService {

    private static final String RESOURCE_DATASET = "DATASET";
    private static final String RESOURCE_DASHBOARD = "DASHBOARD";
    private static final String RESOURCE_CHART = "CHART";
    private static final String RESOURCE_PORTAL = "PORTAL";
    private static final String RESOURCE_BIG_SCREEN = "BIG_SCREEN";
    private static final String RESOURCE_SPREADSHEET = "SPREADSHEET";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Set<String> RESOURCE_TYPES = Set.of(
            RESOURCE_DATASET,
            RESOURCE_DASHBOARD,
            RESOURCE_CHART,
            RESOURCE_PORTAL,
            RESOURCE_BIG_SCREEN,
            RESOURCE_SPREADSHEET);

    private final BiSubscriptionMapper subscriptionMapper;
    private final BiAlertRuleMapper alertRuleMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiMetricMapper metricMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalMapper portalMapper;
    private final BiBigScreenMapper bigScreenMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final BiPermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public BiSubscriptionAdminService(BiSubscriptionMapper subscriptionMapper,
                                      BiAlertRuleMapper alertRuleMapper,
                                      BiDatasetMapper datasetMapper,
                                      BiMetricMapper metricMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiChartMapper chartMapper,
                                      BiPortalMapper portalMapper,
                                      BiBigScreenMapper bigScreenMapper,
                                      BiSpreadsheetMapper spreadsheetMapper,
                                      ObjectProvider<BiPermissionService> permissionServiceProvider,
                                      ObjectMapper objectMapper) {
        this(subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                metricMapper,
                dashboardMapper,
                chartMapper,
                portalMapper,
                bigScreenMapper,
                spreadsheetMapper,
                permissionServiceProvider.getIfAvailable(),
                objectMapper);
    }

    public BiSubscriptionAdminService(BiSubscriptionMapper subscriptionMapper,
                                      BiAlertRuleMapper alertRuleMapper,
                                      BiDatasetMapper datasetMapper,
                                      BiMetricMapper metricMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiChartMapper chartMapper,
                                      BiPortalMapper portalMapper,
                                      BiBigScreenMapper bigScreenMapper,
                                      BiSpreadsheetMapper spreadsheetMapper,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.datasetMapper = datasetMapper;
        this.metricMapper = metricMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.bigScreenMapper = bigScreenMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    public List<BiSubscriptionView> listSubscriptions(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = capLimit(limit);
        return safeList(subscriptionMapper.selectList(new LambdaQueryWrapper<BiSubscriptionDO>()
                        .eq(BiSubscriptionDO::getTenantId, scopedTenantId)
                        .orderByDesc(BiSubscriptionDO::getUpdatedAt)
                        .orderByDesc(BiSubscriptionDO::getId)
                        .last("LIMIT " + capped)))
                .stream()
                .map(this::toSubscriptionView)
                .toList();
    }

    public BiSubscriptionView upsertSubscription(Long tenantId,
                                                 String username,
                                                 String role,
                                                 BiSubscriptionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("subscription command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String key = resourceKey(command.subscriptionKey(), "subscriptionKey");
        ResourceRef resource = resolveResource(scopedTenantId, command.resourceType(), command.resourceKey(), command.resourceId());
        enforceSubscribe(scopedTenantId, resource, username, role);
        validateSchedule(command.schedule());
        validateReceivers(command.receivers());

        BiSubscriptionDO row = subscriptionMapper.selectOne(new LambdaQueryWrapper<BiSubscriptionDO>()
                .eq(BiSubscriptionDO::getTenantId, scopedTenantId)
                .eq(BiSubscriptionDO::getWorkspaceId, resource.workspaceId())
                .eq(BiSubscriptionDO::getSubscriptionKey, key)
                .last("LIMIT 1"));
        if (row == null) {
            row = new BiSubscriptionDO();
            row.setTenantId(scopedTenantId);
            row.setWorkspaceId(resource.workspaceId());
            row.setSubscriptionKey(key);
            row.setCreatedBy(defaultUser(username));
        }
        row.setName(required(command.name(), "name"));
        row.setResourceType(resource.resourceType());
        row.setResourceId(resource.resourceId());
        row.setScheduleJson(json(command.schedule()));
        row.setReceiverJson(json(command.receivers()));
        row.setDeliveryJson(json(command.delivery()));
        row.setEnabled(command.enabled() == null ? Boolean.TRUE : command.enabled());
        if (row.getId() == null) {
            subscriptionMapper.insert(row);
        } else {
            subscriptionMapper.updateById(row);
        }
        BiSubscriptionDO persisted = subscriptionMapper.selectOne(new LambdaQueryWrapper<BiSubscriptionDO>()
                .eq(BiSubscriptionDO::getTenantId, scopedTenantId)
                .eq(BiSubscriptionDO::getWorkspaceId, resource.workspaceId())
                .eq(BiSubscriptionDO::getSubscriptionKey, key)
                .last("LIMIT 1"));
        return toSubscriptionView(persisted == null ? row : persisted);
    }

    public void deleteSubscription(Long tenantId, Long id) {
        BiSubscriptionDO row = subscriptionMapper.selectById(id);
        if (row == null || !normalizeTenant(row.getTenantId()).equals(normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("BI subscription not found: " + id);
        }
        subscriptionMapper.deleteById(id);
    }

    public List<BiAlertRuleView> listAlerts(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = capLimit(limit);
        Map<Long, String> datasetKeys = datasetKeys(scopedTenantId);
        return safeList(alertRuleMapper.selectList(new LambdaQueryWrapper<BiAlertRuleDO>()
                        .eq(BiAlertRuleDO::getTenantId, scopedTenantId)
                        .orderByDesc(BiAlertRuleDO::getUpdatedAt)
                        .orderByDesc(BiAlertRuleDO::getId)
                        .last("LIMIT " + capped)))
                .stream()
                .map(row -> toAlertView(row, datasetKeys.get(row.getDatasetId())))
                .toList();
    }

    public BiAlertRuleView upsertAlert(Long tenantId,
                                       String username,
                                       String role,
                                       BiAlertRuleCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("alert rule command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String key = resourceKey(command.alertKey(), "alertKey");
        BiDatasetDO dataset = resolveDataset(scopedTenantId, command.datasetKey());
        validateMetric(scopedTenantId, dataset.getId(), command.metricKey());
        validateAlertCondition(command.condition());
        validateReceivers(command.receivers());
        enforceDatasetSubscribe(scopedTenantId, dataset, username, role);

        BiAlertRuleDO row = alertRuleMapper.selectOne(new LambdaQueryWrapper<BiAlertRuleDO>()
                .eq(BiAlertRuleDO::getTenantId, scopedTenantId)
                .eq(BiAlertRuleDO::getWorkspaceId, dataset.getWorkspaceId())
                .eq(BiAlertRuleDO::getAlertKey, key)
                .last("LIMIT 1"));
        if (row == null) {
            row = new BiAlertRuleDO();
            row.setTenantId(scopedTenantId);
            row.setWorkspaceId(dataset.getWorkspaceId());
            row.setAlertKey(key);
            row.setCreatedBy(defaultUser(username));
        }
        row.setName(required(command.name(), "name"));
        row.setDatasetId(dataset.getId());
        row.setMetricKey(required(command.metricKey(), "metricKey"));
        row.setConditionJson(json(command.condition()));
        row.setReceiverJson(json(command.receivers()));
        row.setEnabled(command.enabled() == null ? Boolean.TRUE : command.enabled());
        if (row.getId() == null) {
            alertRuleMapper.insert(row);
        } else {
            alertRuleMapper.updateById(row);
        }
        BiAlertRuleDO persisted = alertRuleMapper.selectOne(new LambdaQueryWrapper<BiAlertRuleDO>()
                .eq(BiAlertRuleDO::getTenantId, scopedTenantId)
                .eq(BiAlertRuleDO::getWorkspaceId, dataset.getWorkspaceId())
                .eq(BiAlertRuleDO::getAlertKey, key)
                .last("LIMIT 1"));
        return toAlertView(persisted == null ? row : persisted, dataset.getDatasetKey());
    }

    public void deleteAlert(Long tenantId, Long id) {
        BiAlertRuleDO row = alertRuleMapper.selectById(id);
        if (row == null || !normalizeTenant(row.getTenantId()).equals(normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("BI alert rule not found: " + id);
        }
        alertRuleMapper.deleteById(id);
    }

    private BiSubscriptionView toSubscriptionView(BiSubscriptionDO row) {
        return new BiSubscriptionView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getSubscriptionKey(),
                row.getName(),
                row.getResourceType(),
                resourceKey(row.getResourceType(), row.getResourceId()),
                row.getResourceId(),
                map(row.getScheduleJson()),
                map(row.getReceiverJson()),
                map(row.getDeliveryJson()),
                row.getEnabled(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private BiAlertRuleView toAlertView(BiAlertRuleDO row, String datasetKey) {
        return new BiAlertRuleView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getAlertKey(),
                row.getName(),
                datasetKey,
                row.getDatasetId(),
                row.getMetricKey(),
                map(row.getConditionJson()),
                map(row.getReceiverJson()),
                row.getEnabled(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private ResourceRef resolveResource(Long tenantId, String resourceType, String resourceKey, Long resourceId) {
        String scopedType = enumValue(resourceType, RESOURCE_TYPES, "resourceType");
        if (resourceId != null) {
            return resolveResourceById(tenantId, scopedType, resourceId);
        }
        String key = resourceKey(resourceKey, "resourceKey");
        if (RESOURCE_DATASET.equals(scopedType)) {
            BiDatasetDO dataset = resolveDataset(tenantId, key);
            return new ResourceRef(scopedType, dataset.getId(), dataset.getWorkspaceId());
        }
        if (RESOURCE_DASHBOARD.equals(scopedType)) {
            BiDashboardDO row = dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                    .in(BiDashboardDO::getTenantId, tenantScope(tenantId))
                    .eq(BiDashboardDO::getDashboardKey, key)
                    .ne(BiDashboardDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiDashboardDO::getTenantId)
                    .last("LIMIT 1"));
            if (row == null || row.getId() == null) {
                throw new IllegalArgumentException("BI dashboard not found: " + key);
            }
            return new ResourceRef(scopedType, row.getId(), row.getWorkspaceId());
        }
        if (RESOURCE_CHART.equals(scopedType)) {
            BiChartDO row = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                    .in(BiChartDO::getTenantId, tenantScope(tenantId))
                    .eq(BiChartDO::getChartKey, key)
                    .ne(BiChartDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiChartDO::getTenantId)
                    .last("LIMIT 1"));
            if (row == null || row.getId() == null) {
                throw new IllegalArgumentException("BI chart not found: " + key);
            }
            return new ResourceRef(scopedType, row.getId(), row.getWorkspaceId());
        }
        if (RESOURCE_PORTAL.equals(scopedType)) {
            BiPortalDO row = portalMapper.selectOne(new LambdaQueryWrapper<BiPortalDO>()
                    .in(BiPortalDO::getTenantId, tenantScope(tenantId))
                    .eq(BiPortalDO::getPortalKey, key)
                    .ne(BiPortalDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiPortalDO::getTenantId)
                    .last("LIMIT 1"));
            if (row == null || row.getId() == null) {
                throw new IllegalArgumentException("BI portal not found: " + key);
            }
            return new ResourceRef(scopedType, row.getId(), row.getWorkspaceId());
        }
        if (RESOURCE_BIG_SCREEN.equals(scopedType)) {
            BiBigScreenDO row = bigScreenMapper.selectOne(new LambdaQueryWrapper<BiBigScreenDO>()
                    .in(BiBigScreenDO::getTenantId, tenantScope(tenantId))
                    .eq(BiBigScreenDO::getScreenKey, key)
                    .ne(BiBigScreenDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiBigScreenDO::getTenantId)
                    .last("LIMIT 1"));
            if (row == null || row.getId() == null) {
                throw new IllegalArgumentException("BI big screen not found: " + key);
            }
            return new ResourceRef(scopedType, row.getId(), row.getWorkspaceId());
        }
        BiSpreadsheetDO row = spreadsheetMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetDO>()
                .in(BiSpreadsheetDO::getTenantId, tenantScope(tenantId))
                .eq(BiSpreadsheetDO::getSpreadsheetKey, key)
                .ne(BiSpreadsheetDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiSpreadsheetDO::getTenantId)
                .last("LIMIT 1"));
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + key);
        }
        return new ResourceRef(scopedType, row.getId(), row.getWorkspaceId());
    }

    private ResourceRef resolveResourceById(Long tenantId, String resourceType, Long resourceId) {
        if (RESOURCE_DATASET.equals(resourceType)) {
            BiDatasetDO row = datasetMapper.selectById(resourceId);
            if (row != null && tenantScope(tenantId).contains(normalizeTenant(row.getTenantId()))) {
                return new ResourceRef(resourceType, row.getId(), row.getWorkspaceId());
            }
        }
        if (RESOURCE_DASHBOARD.equals(resourceType)) {
            BiDashboardDO row = dashboardMapper.selectById(resourceId);
            if (row != null && tenantScope(tenantId).contains(normalizeTenant(row.getTenantId()))) {
                return new ResourceRef(resourceType, row.getId(), row.getWorkspaceId());
            }
        }
        if (RESOURCE_CHART.equals(resourceType)) {
            BiChartDO row = chartMapper.selectById(resourceId);
            if (row != null && tenantScope(tenantId).contains(normalizeTenant(row.getTenantId()))) {
                return new ResourceRef(resourceType, row.getId(), row.getWorkspaceId());
            }
        }
        if (RESOURCE_PORTAL.equals(resourceType)) {
            BiPortalDO row = portalMapper.selectById(resourceId);
            if (row != null && tenantScope(tenantId).contains(normalizeTenant(row.getTenantId()))) {
                return new ResourceRef(resourceType, row.getId(), row.getWorkspaceId());
            }
        }
        if (RESOURCE_BIG_SCREEN.equals(resourceType)) {
            BiBigScreenDO row = bigScreenMapper.selectById(resourceId);
            if (row != null && tenantScope(tenantId).contains(normalizeTenant(row.getTenantId()))) {
                return new ResourceRef(resourceType, row.getId(), row.getWorkspaceId());
            }
        }
        if (RESOURCE_SPREADSHEET.equals(resourceType)) {
            BiSpreadsheetDO row = spreadsheetMapper.selectById(resourceId);
            if (row != null && tenantScope(tenantId).contains(normalizeTenant(row.getTenantId()))) {
                return new ResourceRef(resourceType, row.getId(), row.getWorkspaceId());
            }
        }
        throw new IllegalArgumentException("BI resource not found: " + resourceType + "#" + resourceId);
    }

    private BiDatasetDO resolveDataset(Long tenantId, String datasetKey) {
        String key = required(datasetKey, "datasetKey");
        BiDatasetDO dataset = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .eq(BiDatasetDO::getDatasetKey, key)
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiDatasetDO::getTenantId)
                .last("LIMIT 1"));
        if (dataset == null || dataset.getId() == null) {
            throw new IllegalArgumentException("BI dataset not found: " + key);
        }
        return dataset;
    }

    private void validateMetric(Long tenantId, Long datasetId, String metricKey) {
        String key = required(metricKey, "metricKey");
        BiMetricDO metric = metricMapper.selectOne(new LambdaQueryWrapper<BiMetricDO>()
                .in(BiMetricDO::getTenantId, tenantScope(tenantId))
                .eq(BiMetricDO::getDatasetId, datasetId)
                .eq(BiMetricDO::getMetricKey, key)
                .ne(BiMetricDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiMetricDO::getTenantId)
                .last("LIMIT 1"));
        if (metric == null || metric.getId() == null) {
            throw new IllegalArgumentException("BI metric not found: " + key);
        }
    }

    private void enforceSubscribe(Long tenantId, ResourceRef resource, String username, String role) {
        if (permissionService == null) {
            return;
        }
        permissionService.enforceResourceAccess(
                tenantId,
                resource.workspaceId(),
                resource.resourceType(),
                resource.resourceId(),
                new BiQueryContext(tenantId, username, role),
                BiPermissionService.ACTION_SUBSCRIBE);
    }

    private void enforceDatasetSubscribe(Long tenantId, BiDatasetDO dataset, String username, String role) {
        enforceSubscribe(tenantId, new ResourceRef(RESOURCE_DATASET, dataset.getId(), dataset.getWorkspaceId()),
                username, role);
    }

    private String resourceKey(String resourceType, Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        String type = upperDefault(resourceType, "");
        if (RESOURCE_DATASET.equals(type)) {
            BiDatasetDO row = datasetMapper.selectById(resourceId);
            return row == null ? null : row.getDatasetKey();
        }
        if (RESOURCE_DASHBOARD.equals(type)) {
            BiDashboardDO row = dashboardMapper.selectById(resourceId);
            return row == null ? null : row.getDashboardKey();
        }
        if (RESOURCE_CHART.equals(type)) {
            BiChartDO row = chartMapper.selectById(resourceId);
            return row == null ? null : row.getChartKey();
        }
        if (RESOURCE_PORTAL.equals(type)) {
            BiPortalDO row = portalMapper.selectById(resourceId);
            return row == null ? null : row.getPortalKey();
        }
        if (RESOURCE_BIG_SCREEN.equals(type)) {
            BiBigScreenDO row = bigScreenMapper.selectById(resourceId);
            return row == null ? null : row.getScreenKey();
        }
        if (RESOURCE_SPREADSHEET.equals(type)) {
            BiSpreadsheetDO row = spreadsheetMapper.selectById(resourceId);
            return row == null ? null : row.getSpreadsheetKey();
        }
        return null;
    }

    private Map<Long, String> datasetKeys(Long tenantId) {
        Map<Long, String> keys = new LinkedHashMap<>();
        safeList(datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                        .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                        .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)))
                .forEach(row -> keys.putIfAbsent(row.getId(), row.getDatasetKey()));
        return keys;
    }

    private void validateSchedule(Map<String, Object> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            throw new IllegalArgumentException("schedule is required");
        }
        required(String.valueOf(schedule.getOrDefault("frequency", "")), "schedule.frequency");
    }

    private void validateReceivers(Map<String, Object> receivers) {
        if (receivers == null || receivers.isEmpty()) {
            throw new IllegalArgumentException("receivers are required");
        }
        Object channels = receivers.get("channels");
        if (!(channels instanceof List<?> values) || values.isEmpty()) {
            throw new IllegalArgumentException("receivers.channels are required");
        }
    }

    private void validateAlertCondition(Map<String, Object> condition) {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("condition is required");
        }
        String operator = required(String.valueOf(condition.getOrDefault("operator", "")), "condition.operator");
        if (isAnomalyCondition(condition, operator)) {
            positiveInt(condition.get("baselineWindow"), "condition.baselineWindow");
            positiveInt(condition.get("minSamples"), "condition.minSamples");
            positiveNumber(condition.get("sensitivity"), "condition.sensitivity");
            return;
        }
        if (!condition.containsKey("threshold")) {
            throw new IllegalArgumentException("condition.threshold is required");
        }
    }

    private boolean isAnomalyCondition(Map<String, Object> condition, String operator) {
        String normalizedOperator = upperDefault(operator, "");
        String mode = upperDefault(String.valueOf(condition.getOrDefault("mode", condition.getOrDefault("type", ""))), "");
        return normalizedOperator.startsWith("ANOMALY") || "ANOMALY".equals(mode);
    }

    private void positiveInt(Object value, String field) {
        if (value == null) {
            return;
        }
        int parsed;
        if (value instanceof Number number) {
            parsed = number.intValue();
        } else {
            try {
                parsed = Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(field + " must be a positive integer");
            }
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException(field + " must be a positive integer");
        }
    }

    private void positiveNumber(Object value, String field) {
        if (value == null) {
            return;
        }
        double parsed;
        if (value instanceof Number number) {
            parsed = number.doubleValue();
        } else {
            try {
                parsed = Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(field + " must be positive");
            }
        }
        if (parsed <= 0.0) {
            throw new IllegalArgumentException(field + " must be positive");
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

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI subscription payload", e);
        }
    }

    private String enumValue(String value, Set<String> allowed, String field) {
        String normalized = upperDefault(required(value, field), "");
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("unsupported " + field + ": " + value);
        }
        return normalized;
    }

    private String resourceKey(String value, String field) {
        String key = required(value, field);
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException(field + " contains unsafe characters");
        }
        return key;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String upperDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private int capLimit(int limit) {
        return Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        return scopedTenantId == 0L ? List.of(0L) : List.of(scopedTenantId, 0L);
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private record ResourceRef(String resourceType, Long resourceId, Long workspaceId) {
    }
}
