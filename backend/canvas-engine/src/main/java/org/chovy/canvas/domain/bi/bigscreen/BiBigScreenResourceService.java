package org.chovy.canvas.domain.bi.bigscreen;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiBigScreenVersionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiBigScreenVersionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BiBigScreenResourceService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final BiWorkspaceMapper workspaceMapper;
    private final BiBigScreenMapper screenMapper;
    private final BiBigScreenVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public BiBigScreenResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiBigScreenMapper screenMapper,
                                      BiBigScreenVersionMapper versionMapper,
                                      ObjectMapper objectMapper) {
        this.workspaceMapper = workspaceMapper;
        this.screenMapper = screenMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
    }

    public List<BiBigScreenResource> list(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(screenMapper.selectList(new LambdaQueryWrapper<BiBigScreenDO>()
                        .eq(BiBigScreenDO::getTenantId, scopedTenantId)
                        .eq(BiBigScreenDO::getWorkspaceId, workspaceId)
                        .ne(BiBigScreenDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiBigScreenDO::getUpdatedAt)
                        .orderByAsc(BiBigScreenDO::getScreenKey)))
                .stream()
                .map(row -> toResource(row, row.getStatus()))
                .toList();
    }

    public BiBigScreenResource get(Long tenantId, String screenKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiBigScreenDO row = find(scopedTenantId, workspaceId, screenKey);
        if (row == null) {
            throw new IllegalArgumentException("BI big screen not found: " + screenKey);
        }
        return toResource(row, row.getStatus());
    }

    public BiBigScreenResource saveDraft(Long tenantId, String username, BiBigScreenResource resource) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiBigScreenDO existing = find(scopedTenantId, workspaceId, resource.screenKey());
        int version = existing == null ? 1 : value(existing.getVersion(), 1);
        BiBigScreenDO row = new BiBigScreenDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setScreenKey(required(resource.screenKey(), "screenKey"));
        row.setName(required(resource.name(), "name"));
        row.setDescription(resource.description());
        row.setSizeJson(json(resource.size()));
        row.setBackgroundJson(json(resource.background()));
        row.setLayoutJson(json(resource.layout()));
        row.setRefreshJson(json(resource.refresh()));
        row.setMobileLayoutJson(json(resource.mobileLayout()));
        row.setStatus(STATUS_DRAFT);
        row.setVersion(version);
        row.setCreatedBy(defaultUser(username));
        screenMapper.upsert(row);
        BiBigScreenDO persisted = find(scopedTenantId, workspaceId, resource.screenKey());
        return toResource(persisted == null ? row : persisted, STATUS_DRAFT);
    }

    public BiBigScreenResource publish(Long tenantId, String username, String screenKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiBigScreenDO row = find(scopedTenantId, workspaceId, screenKey);
        if (row == null) {
            throw new IllegalArgumentException("BI big screen not found: " + screenKey);
        }
        screenMapper.publish(scopedTenantId, workspaceId, screenKey);
        BiBigScreenDO published = find(scopedTenantId, workspaceId, screenKey);
        BiBigScreenDO effective = published == null ? row : published;
        if (published == null) {
            effective.setStatus(STATUS_PUBLISHED);
            effective.setVersion(value(row.getVersion(), 1) + 1);
        }
        BiBigScreenResource resource = toResource(effective, STATUS_PUBLISHED);
        insertVersion(scopedTenantId, workspaceId, effective, resource, username);
        return resource;
    }

    public BiBigScreenResource archive(Long tenantId, String screenKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiBigScreenDO row = find(scopedTenantId, workspaceId, screenKey);
        if (row == null) {
            throw new IllegalArgumentException("BI big screen not found: " + screenKey);
        }
        screenMapper.archive(scopedTenantId, workspaceId, screenKey);
        BiBigScreenDO archived = find(scopedTenantId, workspaceId, screenKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return toResource(row, STATUS_ARCHIVED);
        }
        return toResource(archived, STATUS_ARCHIVED);
    }

    public List<BiBigScreenVersionView> listVersions(Long tenantId, String screenKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiBigScreenDO row = find(scopedTenantId, workspaceId, screenKey);
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiBigScreenVersionDO>()
                        .eq(BiBigScreenVersionDO::getTenantId, scopedTenantId)
                        .eq(BiBigScreenVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiBigScreenVersionDO::getScreenId, row.getId())
                        .orderByDesc(BiBigScreenVersionDO::getVersion)
                        .last("LIMIT " + capped)))
                .stream()
                .map(this::toVersionView)
                .toList();
    }

    public BiBigScreenResource restoreVersion(Long tenantId, String username, String screenKey, int version) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        if (find(scopedTenantId, workspaceId, screenKey) == null) {
            throw new IllegalArgumentException("BI big screen not found: " + screenKey);
        }
        BiBigScreenVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiBigScreenVersionDO>()
                .eq(BiBigScreenVersionDO::getTenantId, scopedTenantId)
                .eq(BiBigScreenVersionDO::getWorkspaceId, workspaceId)
                .eq(BiBigScreenVersionDO::getScreenKey, screenKey)
                .eq(BiBigScreenVersionDO::getVersion, version));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI big screen version not found: " + screenKey + "#" + version);
        }
        return saveDraft(scopedTenantId, username, resource(snapshot.getResourceJson()));
    }

    private BiBigScreenVersionView toVersionView(BiBigScreenVersionDO row) {
        return new BiBigScreenVersionView(
                row.getId(),
                row.getScreenKey(),
                row.getVersion(),
                row.getStatus(),
                resource(row.getResourceJson()),
                row.getPublishedBy(),
                row.getCreatedAt());
    }

    private void insertVersion(Long tenantId,
                               Long workspaceId,
                               BiBigScreenDO row,
                               BiBigScreenResource resource,
                               String username) {
        if (versionMapper == null) {
            return;
        }
        BiBigScreenVersionDO version = new BiBigScreenVersionDO();
        version.setTenantId(tenantId);
        version.setWorkspaceId(workspaceId);
        version.setScreenId(row.getId());
        version.setScreenKey(row.getScreenKey());
        version.setVersion(value(row.getVersion(), 1));
        version.setStatus(STATUS_PUBLISHED);
        version.setResourceJson(json(resource));
        version.setPublishedBy(defaultUser(username));
        versionMapper.insert(version);
    }

    private BiBigScreenResource toResource(BiBigScreenDO row, String status) {
        return new BiBigScreenResource(
                row.getId(),
                row.getScreenKey(),
                row.getName(),
                row.getDescription(),
                map(row.getSizeJson()),
                map(row.getBackgroundJson()),
                list(row.getLayoutJson()),
                map(row.getRefreshJson()),
                map(row.getMobileLayoutJson()),
                status,
                value(row.getVersion(), 1),
                "PERSISTED");
    }

    private BiBigScreenResource resource(String json) {
        try {
            return objectMapper.readValue(json, BiBigScreenResource.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("resourceJson must be a valid BI big screen resource", e);
        }
    }

    private BiBigScreenDO find(Long tenantId, Long workspaceId, String screenKey) {
        return screenMapper.selectOne(new LambdaQueryWrapper<BiBigScreenDO>()
                .eq(BiBigScreenDO::getTenantId, tenantId)
                .eq(BiBigScreenDO::getWorkspaceId, workspaceId)
                .eq(BiBigScreenDO::getScreenKey, required(screenKey, "screenKey")));
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
            return objectMapper.readValue(emptyObject(json), new TypeReference<Map<String, Object>>() {
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

    private String emptyObject(String json) {
        return json == null || json.isBlank() ? "{}" : json;
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
