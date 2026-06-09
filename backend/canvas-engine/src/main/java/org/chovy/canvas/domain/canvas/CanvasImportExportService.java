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

/**
 * CanvasImportExportService 编排 domain.canvas 场景的领域业务规则。
 */
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

    /**
     * 导出指定 Canvas 版本为可迁移包。
     * 方法会校验版本归属画布，移除运行态字段并脱敏敏感配置，返回包元数据、画布元数据和清洗后的图结构。
     */
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
    /**
     * 导入 Canvas 包并创建无显式租户绑定的草稿画布。
     * 该重载用于兼容旧入口，实际写入逻辑委托给带租户参数的方法。
     */
    public CanvasImportResp importCanvas(CanvasImportReq req) {
        return importCanvas(req, null);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 在指定租户下导入 Canvas 包。
     * 方法校验包版本和图结构，创建新的草稿 Canvas 与第 1 版草稿，并在包内存在项目/文件夹元数据时一并写入。
     */
    public CanvasImportResp importCanvas(CanvasImportReq req, Long tenantId) {
        Map<String, Object> pkg = parseMap(req.packageJson());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CanvasImportResp(canvas, version.getId());
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireCanvas 流程生成的业务结果。
     */
    private CanvasDO requireCanvas(Long canvasId) {
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas not found: " + canvasId);
        }
        return canvas;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    private CanvasProjectFolderDO findMetadata(Long canvasId) {
        return projectFolderMapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getCanvasId, canvasId)
                .last("LIMIT 1"));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param canvas canvas 参数，用于 canvasMeta 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 canvasMeta 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfNotNull 流程中的校验、计算或对象转换。
     * @param map map 参数，用于 putIfNotNull 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * 执行 sourceMeta 流程，围绕 source meta 完成校验、计算或结果组装。
     *
     * @param canvas canvas 参数，用于 sourceMeta 流程中的校验、计算或对象转换。
     * @param version version 参数，用于 sourceMeta 流程中的校验、计算或对象转换。
     * @return 返回 sourceMeta 流程生成的业务结果。
     */
    private Map<String, Object> sourceMeta(CanvasDO canvas, CanvasVersionDO version) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("canvasId", canvas.getId());
        source.put("canvasName", canvas.getName());
        source.put("versionId", version.getId());
        source.put("version", version.getVersion());
        return source;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param String string 参数，用于 saveMetadataIfPresent 流程中的校验、计算或对象转换。
     * @param canvasMeta canvas meta 参数，用于 saveMetadataIfPresent 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     */
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

    /**
     * 解析并校验输入数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Canvas package JSON is required");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("Canvas package JSON parse failed: " + e.getMessage(), e);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param required required 参数，用于 objectMap 流程中的校验、计算或对象转换。
     * @return 返回 objectMap 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value, String field, boolean required) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null && !required) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Canvas package field must be an object: " + field);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        ((Map<Object, Object>) map).forEach((key, item) -> {
            if (key != null) {
                result.put(String.valueOf(key), item);
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 sanitizeGraph 流程，围绕 sanitize graph 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 sanitizeGraph 流程中的校验、计算或对象转换。
     * @param graph graph 参数，用于 sanitizeGraph 流程中的校验、计算或对象转换。
     * @return 返回 sanitizeGraph 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeGraph(Map<String, Object> graph) {
        Object sanitized = sanitizeValue(graph);
        Object masked = DataMaskingUtil.maskObject(sanitized, EXPORT_MASK_KEYS);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(masked instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Canvas graph must be an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        ((Map<Object, Object>) map).forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 sanitizeValue 流程，围绕 sanitize value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sanitizeValue 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Object sanitizeValue(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 removed key 的布尔判断结果。
     */
    private boolean removedKey(String key) {
        String normalized = key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return REMOVED_EXACT_KEYS.contains(normalized)
                || REMOVED_KEY_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param String string 参数，用于 validateGraph 流程中的校验、计算或对象转换。
     * @param graph graph 参数，用于 validateGraph 流程中的校验、计算或对象转换。
     */
    private void validateGraph(Map<String, Object> graph) {
        Object nodes = graph.get("nodes");
        if (!(nodes instanceof List<?>)) {
            throw new IllegalArgumentException("Canvas package graph.nodes must be an array");
        }
    }

    /**
     * 执行 asInt 流程，围绕 as int 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 as int 计算得到的数量、金额或指标值。
     */
    private Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text.trim());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 string 流程中的校验、计算或对象转换。
     * @param map map 参数，用于 string 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param fallback fallback 参数，用于 string 流程中的校验、计算或对象转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return stringValue(value, fallback);
    }

    /**
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 stringValue 流程中的校验、计算或对象转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param String string 参数，用于 writeJson 流程中的校验、计算或对象转换。
     * @param graph graph 参数，用于 writeJson 流程中的校验、计算或对象转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Map<String, Object> graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("Canvas package graph serialization failed: " + e.getMessage(), e);
        }
    }

}
