package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dto.canvas.CanvasExportPackage;
import org.chovy.canvas.dto.canvas.CanvasImportReq;
import org.chovy.canvas.dto.canvas.CanvasImportResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CanvasImportExportService {

    private static final int PACKAGE_VERSION = 1;
    private static final Set<String> REMOVED_EXACT_KEYS = Set.of(
            "audiencesnapshotid",
            "idempotencykey",
            "publishedversionid",
            "canaryversionid",
            "previousversionid",
            "routestate",
            "executionid",
            "requestid",
            "sendrecordid",
            "messagesendrecordid");
    private static final List<String> REMOVED_KEY_FRAGMENTS = List.of(
            "apikey", "token", "secret", "password", "authorization", "cookie", "credential");
    private static final Set<String> EXPORT_MASK_KEYS = Set.of(
            "phone", "mobile", "phoneNumber", "mobileNumber",
            "idCard", "idNumber", "identityCard",
            "bankCard", "cardNumber",
            "password", "passwd", "pwd",
            "token", "accessToken", "refreshToken",
            "secret", "apiKey", "authorization",
            "cookie", "session", "credential");

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper versionMapper;
    private final CanvasProjectFolderMapper projectFolderMapper;
    private final ObjectMapper objectMapper;

    public CanvasExportPackage exportCanvas(Long canvasId, Long versionId) {
        CanvasDO canvas = requireCanvas(canvasId);
        CanvasVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !canvasId.equals(version.getCanvasId())) {
            throw new IllegalArgumentException("Canvas version not found: " + versionId);
        }

        Map<String, Object> graph = sanitizeGraph(parseMap(version.getGraphJson()));
        return new CanvasExportPackage(
                PACKAGE_VERSION,
                LocalDateTime.now(),
                sourceMeta(canvas, version),
                canvasMeta(canvas, findMetadata(canvasId)),
                graph);
    }

    @Transactional(rollbackFor = Exception.class)
    public CanvasImportResp importCanvas(CanvasImportReq req) {
        return importCanvas(req, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public CanvasImportResp importCanvas(CanvasImportReq req, Long tenantId) {
        Map<String, Object> pkg = parseMap(req.packageJson());
        if (!Integer.valueOf(PACKAGE_VERSION).equals(asInt(pkg.get("packageVersion")))) {
            throw new IllegalArgumentException("Unsupported canvas package version: " + pkg.get("packageVersion"));
        }

        Map<String, Object> canvasMeta = objectMap(pkg.get("canvas"), "canvas", false);
        Map<String, Object> graph = objectMap(pkg.get("graph"), "graph", true);
        validateGraph(graph);

        String operator = stringValue(req.operator(), "system");
        CanvasDO canvas = new CanvasDO();
        canvas.setTenantId(tenantId);
        canvas.setName(string(canvasMeta, "name", "Imported Canvas"));
        canvas.setDescription(string(canvasMeta, "description", null));
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvas.setCreatedBy(operator);
        canvas.setProjectKey(string(canvasMeta, "projectKey", null));
        canvas.setProjectName(string(canvasMeta, "projectName", null));
        canvas.setFolderKey(string(canvasMeta, "folderKey", null));
        canvas.setFolderName(string(canvasMeta, "folderName", null));
        canvas.setTriggerType(string(canvasMeta, "triggerType", null));
        canvas.setCronExpression(string(canvasMeta, "cronExpression", null));
        canvasMapper.insert(canvas);

        CanvasVersionDO version = new CanvasVersionDO();
        version.setTenantId(tenantId);
        version.setCanvasId(canvas.getId());
        version.setVersion(1);
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setCreatedBy(operator);
        version.setGraphJson(writeJson(graph));
        versionMapper.insert(version);

        saveMetadataIfPresent(canvas.getId(), canvasMeta, operator);
        return new CanvasImportResp(canvas, version.getId());
    }

    private CanvasDO requireCanvas(Long canvasId) {
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas not found: " + canvasId);
        }
        return canvas;
    }

    private CanvasProjectFolderDO findMetadata(Long canvasId) {
        return projectFolderMapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getCanvasId, canvasId)
                .last("LIMIT 1"));
    }

    private Map<String, Object> canvasMeta(CanvasDO canvas, CanvasProjectFolderDO metadata) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", canvas.getName());
        meta.put("description", canvas.getDescription());
        meta.put("triggerType", canvas.getTriggerType());
        meta.put("cronExpression", canvas.getCronExpression());
        putIfNotNull(meta, "projectKey", canvas.getProjectKey());
        putIfNotNull(meta, "projectName", canvas.getProjectName());
        putIfNotNull(meta, "folderKey", canvas.getFolderKey());
        putIfNotNull(meta, "folderName", canvas.getFolderName());
        if (metadata != null) {
            putIfNotNull(meta, "projectKey", metadata.getProjectKey());
            putIfNotNull(meta, "projectName", metadata.getProjectName());
            putIfNotNull(meta, "folderKey", metadata.getFolderKey());
            putIfNotNull(meta, "folderName", metadata.getFolderName());
        }
        return meta;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private Map<String, Object> sourceMeta(CanvasDO canvas, CanvasVersionDO version) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("canvasId", canvas.getId());
        source.put("canvasName", canvas.getName());
        source.put("versionId", version.getId());
        source.put("version", version.getVersion());
        return source;
    }

    private void saveMetadataIfPresent(Long canvasId, Map<String, Object> canvasMeta, String operator) {
        String projectKey = string(canvasMeta, "projectKey", null);
        String projectName = string(canvasMeta, "projectName", null);
        String folderKey = string(canvasMeta, "folderKey", null);
        String folderName = string(canvasMeta, "folderName", null);
        if (projectKey == null && projectName == null && folderKey == null && folderName == null) {
            return;
        }
        CanvasProjectFolderDO row = new CanvasProjectFolderDO();
        row.setCanvasId(canvasId);
        row.setProjectKey(projectKey);
        row.setProjectName(projectName);
        row.setFolderKey(folderKey);
        row.setFolderName(folderName);
        row.setUpdatedBy(operator);
        projectFolderMapper.insert(row);
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Canvas package JSON is required");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Canvas package JSON parse failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value, String field, boolean required) {
        if (value == null && !required) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Canvas package field must be an object: " + field);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, item) -> {
            if (key != null) {
                result.put(String.valueOf(key), item);
            }
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeGraph(Map<String, Object> graph) {
        Object sanitized = sanitizeValue(graph);
        Object masked = DataMaskingUtil.maskObject(sanitized, EXPORT_MASK_KEYS);
        if (!(masked instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Canvas graph must be an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            ((Map<Object, Object>) map).forEach((key, item) -> {
                if (key != null && !removedKey(String.valueOf(key))) {
                    sanitized.put(String.valueOf(key), sanitizeValue(item));
                }
            });
            return sanitized;
        }
        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>(list.size());
            for (Object item : list) {
                sanitized.add(sanitizeValue(item));
            }
            return sanitized;
        }
        return value;
    }

    private boolean removedKey(String key) {
        String normalized = key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return REMOVED_EXACT_KEYS.contains(normalized)
                || REMOVED_KEY_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    private void validateGraph(Map<String, Object> graph) {
        Object nodes = graph.get("nodes");
        if (!(nodes instanceof List<?>)) {
            throw new IllegalArgumentException("Canvas package graph.nodes must be an array");
        }
    }

    private Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String string(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return stringValue(value, fallback);
    }

    private String stringValue(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    private String writeJson(Map<String, Object> graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        } catch (Exception e) {
            throw new IllegalArgumentException("Canvas package graph serialization failed: " + e.getMessage(), e);
        }
    }

}
