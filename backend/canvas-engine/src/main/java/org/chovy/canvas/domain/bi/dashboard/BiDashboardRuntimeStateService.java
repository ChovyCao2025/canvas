package org.chovy.canvas.domain.bi.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDashboardRuntimeStateDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDashboardRuntimeStateMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class BiDashboardRuntimeStateService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final TypeReference<Map<String, Object>> PARAMETER_MAP = new TypeReference<>() {
    };

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDashboardRuntimeStateMapper stateMapper;
    private final ObjectMapper objectMapper;

    public BiDashboardRuntimeStateService(BiWorkspaceMapper workspaceMapper,
                                          BiDashboardRuntimeStateMapper stateMapper,
                                          ObjectMapper objectMapper) {
        this.workspaceMapper = workspaceMapper;
        this.stateMapper = stateMapper;
        this.objectMapper = objectMapper;
    }

    public BiDashboardRuntimeStateView get(Long tenantId, String username, String dashboardKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        String user = defaultUser(username);
        String key = resourceKey(dashboardKey, "dashboardKey");
        BiDashboardRuntimeStateDO row = stateMapper.selectOne(new LambdaQueryWrapper<BiDashboardRuntimeStateDO>()
                .eq(BiDashboardRuntimeStateDO::getTenantId, scopedTenantId)
                .eq(BiDashboardRuntimeStateDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardRuntimeStateDO::getDashboardKey, key)
                .eq(BiDashboardRuntimeStateDO::getUsername, user)
                .last("LIMIT 1"));
        if (row == null) {
            return new BiDashboardRuntimeStateView(key, user, Map.of(), null);
        }
        return toView(row);
    }

    public BiDashboardRuntimeStateView save(Long tenantId,
                                            String username,
                                            String dashboardKey,
                                            BiDashboardRuntimeStateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("dashboard runtime state command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        String user = defaultUser(username);
        String key = resourceKey(dashboardKey, "dashboardKey");
        LocalDateTime now = LocalDateTime.now();
        BiDashboardRuntimeStateDO row = new BiDashboardRuntimeStateDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setDashboardKey(key);
        row.setUsername(user);
        row.setParameterJson(json(command.parameters()));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        stateMapper.upsert(row);
        return new BiDashboardRuntimeStateView(key, user, command.parameters(), now);
    }

    private BiDashboardRuntimeStateView toView(BiDashboardRuntimeStateDO row) {
        return new BiDashboardRuntimeStateView(
                row.getDashboardKey(),
                row.getUsername(),
                parameters(row.getParameterJson()),
                row.getUpdatedAt());
    }

    private Map<String, Object> parameters(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, PARAMETER_MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to parse BI dashboard runtime parameters", e);
        }
    }

    private String json(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters == null ? Map.of() : parameters);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to serialize BI dashboard runtime parameters", e);
        }
    }

    private Long workspaceId(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .eq(BiWorkspaceDO::getTenantId, tenantId)
                .eq(BiWorkspaceDO::getWorkspaceKey, WORKSPACE_KEY)
                .last("LIMIT 1"));
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalStateException("BI workspace not found: " + WORKSPACE_KEY);
        }
        return workspace.getId();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private String resourceKey(String value, String fieldName) {
        if (value == null || value.isBlank() || !RESOURCE_KEY.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must match " + RESOURCE_KEY.pattern());
        }
        return value;
    }
}
