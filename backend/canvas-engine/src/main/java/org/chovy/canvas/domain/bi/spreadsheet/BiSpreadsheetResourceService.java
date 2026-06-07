package org.chovy.canvas.domain.bi.spreadsheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetVersionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetVersionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BiSpreadsheetResourceService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final BiWorkspaceMapper workspaceMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final BiSpreadsheetVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public BiSpreadsheetResourceService(BiWorkspaceMapper workspaceMapper,
                                        BiSpreadsheetMapper spreadsheetMapper,
                                        BiSpreadsheetVersionMapper versionMapper,
                                        ObjectMapper objectMapper) {
        this.workspaceMapper = workspaceMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
    }

    public List<BiSpreadsheetResource> list(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(spreadsheetMapper.selectList(new LambdaQueryWrapper<BiSpreadsheetDO>()
                        .eq(BiSpreadsheetDO::getTenantId, scopedTenantId)
                        .eq(BiSpreadsheetDO::getWorkspaceId, workspaceId)
                        .ne(BiSpreadsheetDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiSpreadsheetDO::getUpdatedAt)
                        .orderByAsc(BiSpreadsheetDO::getSpreadsheetKey)))
                .stream()
                .map(row -> toResource(row, row.getStatus()))
                .toList();
    }

    public BiSpreadsheetResource get(Long tenantId, String spreadsheetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        return toResource(row, row.getStatus());
    }

    public BiSpreadsheetResource saveDraft(Long tenantId, String username, BiSpreadsheetResource resource) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO existing = find(scopedTenantId, workspaceId, resource.spreadsheetKey());
        int version = existing == null ? 1 : value(existing.getVersion(), 1);
        BiSpreadsheetDO row = new BiSpreadsheetDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setSpreadsheetKey(required(resource.spreadsheetKey(), "spreadsheetKey"));
        row.setName(required(resource.name(), "name"));
        row.setDescription(resource.description());
        row.setSheetJson(json(resource.sheets()));
        row.setDataBindingJson(json(resource.dataBinding()));
        row.setStyleJson(json(resource.style()));
        row.setStatus(STATUS_DRAFT);
        row.setVersion(version);
        row.setCreatedBy(defaultUser(username));
        spreadsheetMapper.upsert(row);
        BiSpreadsheetDO persisted = find(scopedTenantId, workspaceId, resource.spreadsheetKey());
        return toResource(persisted == null ? row : persisted, STATUS_DRAFT);
    }

    public BiSpreadsheetResource publish(Long tenantId, String username, String spreadsheetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        spreadsheetMapper.publish(scopedTenantId, workspaceId, spreadsheetKey);
        BiSpreadsheetDO published = find(scopedTenantId, workspaceId, spreadsheetKey);
        BiSpreadsheetDO effective = published == null ? row : published;
        if (published == null) {
            effective.setStatus(STATUS_PUBLISHED);
            effective.setVersion(value(row.getVersion(), 1) + 1);
        }
        BiSpreadsheetResource resource = toResource(effective, STATUS_PUBLISHED);
        insertVersion(scopedTenantId, workspaceId, effective, resource, username);
        return resource;
    }

    public BiSpreadsheetResource archive(Long tenantId, String spreadsheetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        spreadsheetMapper.archive(scopedTenantId, workspaceId, spreadsheetKey);
        BiSpreadsheetDO archived = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return toResource(row, STATUS_ARCHIVED);
        }
        return toResource(archived, STATUS_ARCHIVED);
    }

    public List<BiSpreadsheetVersionView> listVersions(Long tenantId, String spreadsheetKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiSpreadsheetVersionDO>()
                        .eq(BiSpreadsheetVersionDO::getTenantId, scopedTenantId)
                        .eq(BiSpreadsheetVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiSpreadsheetVersionDO::getSpreadsheetId, row.getId())
                        .orderByDesc(BiSpreadsheetVersionDO::getVersion)
                        .last("LIMIT " + capped)))
                .stream()
                .map(this::toVersionView)
                .toList();
    }

    public BiSpreadsheetResource restoreVersion(Long tenantId, String username, String spreadsheetKey, int version) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        if (find(scopedTenantId, workspaceId, spreadsheetKey) == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        BiSpreadsheetVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetVersionDO>()
                .eq(BiSpreadsheetVersionDO::getTenantId, scopedTenantId)
                .eq(BiSpreadsheetVersionDO::getWorkspaceId, workspaceId)
                .eq(BiSpreadsheetVersionDO::getSpreadsheetKey, spreadsheetKey)
                .eq(BiSpreadsheetVersionDO::getVersion, version));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI spreadsheet version not found: " + spreadsheetKey + "#" + version);
        }
        return saveDraft(scopedTenantId, username, resource(snapshot.getResourceJson()));
    }

    private BiSpreadsheetVersionView toVersionView(BiSpreadsheetVersionDO row) {
        return new BiSpreadsheetVersionView(
                row.getId(),
                row.getSpreadsheetKey(),
                row.getVersion(),
                row.getStatus(),
                resource(row.getResourceJson()),
                row.getPublishedBy(),
                row.getCreatedAt());
    }

    private void insertVersion(Long tenantId,
                               Long workspaceId,
                               BiSpreadsheetDO row,
                               BiSpreadsheetResource resource,
                               String username) {
        if (versionMapper == null) {
            return;
        }
        BiSpreadsheetVersionDO version = new BiSpreadsheetVersionDO();
        version.setTenantId(tenantId);
        version.setWorkspaceId(workspaceId);
        version.setSpreadsheetId(row.getId());
        version.setSpreadsheetKey(row.getSpreadsheetKey());
        version.setVersion(value(row.getVersion(), 1));
        version.setStatus(STATUS_PUBLISHED);
        version.setResourceJson(json(resource));
        version.setPublishedBy(defaultUser(username));
        versionMapper.insert(version);
    }

    private BiSpreadsheetResource toResource(BiSpreadsheetDO row, String status) {
        return new BiSpreadsheetResource(
                row.getId(),
                row.getSpreadsheetKey(),
                row.getName(),
                row.getDescription(),
                list(row.getSheetJson()),
                map(row.getDataBindingJson()),
                map(row.getStyleJson()),
                status,
                value(row.getVersion(), 1),
                "PERSISTED");
    }

    private BiSpreadsheetResource resource(String json) {
        try {
            return objectMapper.readValue(json, BiSpreadsheetResource.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("resourceJson must be a valid BI spreadsheet resource", e);
        }
    }

    private BiSpreadsheetDO find(Long tenantId, Long workspaceId, String spreadsheetKey) {
        return spreadsheetMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetDO>()
                .eq(BiSpreadsheetDO::getTenantId, tenantId)
                .eq(BiSpreadsheetDO::getWorkspaceId, workspaceId)
                .eq(BiSpreadsheetDO::getSpreadsheetKey, required(spreadsheetKey, "spreadsheetKey")));
    }

    private Long workspaceId(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .eq(BiWorkspaceDO::getTenantId, tenantId)
                .eq(BiWorkspaceDO::getWorkspaceKey, WORKSPACE_KEY));
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalStateException("BI workspace not found: " + WORKSPACE_KEY);
        }
        return workspace.getId();
    }

    private Map<String, Object> map(String json) {
        try {
            return objectMapper.readValue(json == null || json.isBlank() ? "{}" : json,
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON object field is invalid", e);
        }
    }

    private List<Map<String, Object>> list(String json) {
        try {
            return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON array field is invalid", e);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("resource field must be JSON serializable", e);
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username.trim();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
