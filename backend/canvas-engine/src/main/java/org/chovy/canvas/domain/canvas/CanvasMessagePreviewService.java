package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dto.canvas.MessagePreviewReq;
import org.chovy.canvas.dto.canvas.MessagePreviewResp;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CanvasMessagePreviewService 编排 domain.canvas 场景的领域业务规则。
 */
@Service
@RequiredArgsConstructor
public class CanvasMessagePreviewService {

    private static final List<String> CONTENT_FIELDS = List.of(
            "subject", "previewText", "title", "body", "content",
            "imageUrl", "clickUrl", "fromName", "fromEmail");
    private static final List<String> KNOWN_CONFIG_FIELDS = List.of(
            "channel", "templateId", "template_id", "variables", "variablesMapping",
            "subject", "previewText", "title", "body", "content",
            "imageUrl", "clickUrl", "fromName", "fromEmail");
    private static final Pattern DOLLAR_TOKEN =
            Pattern.compile("\\$\\.?([A-Za-z_][A-Za-z0-9_.-]*)");
    private static final Pattern HANDLEBARS_TOKEN =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_.-]*)\\s*}}");
    private static final Set<String> PREVIEW_SENSITIVE_KEYS = Set.of(
            "phone", "mobile", "phoneNumber", "mobileNumber",
            "idCard", "idNumber", "identityCard",
            "bankCard", "cardNumber",
            "password", "passwd", "pwd",
            "token", "accessToken", "refreshToken",
            "secret", "apiKey", "authorization",
            "cookie", "session", "credential");

    private final ObjectMapper objectMapper;

    /**
     * 基于请求中的图 JSON 和上下文生成发送消息节点预览。
     * 方法只解析 SEND_MESSAGE 节点并替换变量，输出内容和变量会进行敏感字段脱敏；该预览不会创建发送记录或触发外部渠道。
     */
    public MessagePreviewResp preview(MessagePreviewReq req) {
        Map<String, Object> root = parseGraph(req.graphJson());
        Map<String, Object> node = findNode(root, req.nodeId());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!NodeType.SEND_MESSAGE.equals(nodeType(node))) {
            throw new IllegalArgumentException("Message preview requires a SEND_MESSAGE node: " + req.nodeId());
        }

        Map<String, Object> config = mergedConfig(node);
        Map<String, Object> context = previewContext(req);
        List<String> warnings = new ArrayList<>();
        warnings.add("PREVIEW_ONLY_NO_SEND");

        Map<String, Object> content = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String field : CONTENT_FIELDS) {
            if (config.containsKey(field)) {
                content.put(field, resolveAny(config.get(field), context, warnings));
            }
        }

        Map<String, Object> variables = resolveVariables(
                config.getOrDefault("variables", config.get("variablesMapping")),
                context,
                warnings);

        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MessagePreviewResp(
                string(config, "channel", "EMAIL").toUpperCase(),
                string(config, "templateId", string(config, "template_id", null)),
                maskedMap(content),
                maskedMap(variables),
                List.copyOf(new LinkedHashSet<>(warnings)));
    }

    /**
     * 解析并校验输入数据。
     *
     * @param graphJson JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Map<String, Object> parseGraph(String graphJson) {
        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalArgumentException("Message preview requires graphJson");
        }
        try {
            return objectMapper.readValue(graphJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("Message preview graphJson parse failed: " + e.getMessage(), e);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param String string 参数，用于 findNode 流程中的校验、计算或对象转换。
     * @param root root 参数，用于 findNode 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findNode(Map<String, Object> root, String nodeId) {
        Object rawNodes = root.get("nodes");
        if (!(rawNodes instanceof List<?> nodes)) {
            throw new IllegalArgumentException("Message preview graph requires nodes");
        }
        if (nodeId != null && !nodeId.isBlank()) {
            // When the editor supplies a nodeId, preview must be scoped to that exact node even if others can send messages.
            for (Object candidate : nodes) {
                if (candidate instanceof Map<?, ?> map && nodeId.equals(String.valueOf(map.get("id")))) {
                    return (Map<String, Object>) map;
                }
            }
            throw new IllegalArgumentException("Message preview node not found: " + nodeId);
        }

        List<Map<String, Object>> sendNodes = new ArrayList<>();
        for (Object candidate : nodes) {
            if (candidate instanceof Map<?, ?> map && NodeType.SEND_MESSAGE.equals(nodeType(map))) {
                sendNodes.add((Map<String, Object>) map);
            }
        }
        if (sendNodes.size() != 1) {
            throw new IllegalArgumentException("Message preview requires one selected SEND_MESSAGE node");
        }
        return sendNodes.get(0);
    }

    /**
     * 执行 nodeType 流程，围绕 node type 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 nodeType 流程中的校验、计算或对象转换。
     * @return 返回 node type 生成的文本或业务键。
     */
    private String nodeType(Map<?, ?> node) {
        Object direct = node.get("type");
        if (direct != null) {
            return String.valueOf(direct);
        }
        Map<String, Object> data = objectMap(node.get("data"));
        Object dataType = data.getOrDefault("type", data.get("typeKey"));
        return dataType == null ? null : String.valueOf(dataType);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 mergedConfig 流程中的校验、计算或对象转换。
     * @param node node 参数，用于 mergedConfig 流程中的校验、计算或对象转换。
     * @return 返回 mergedConfig 流程生成的业务结果。
     */
    private Map<String, Object> mergedConfig(Map<String, Object> node) {
        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> data = objectMap(node.get("data"));
        // React Flow nodes can store config under data, root, legacy config, or bizConfig; later locations override earlier ones.
        merge(config, objectMap(data.get("bizConfig")));
        merge(config, objectMap(data.get("config")));
        copyKnown(data, config);
        merge(config, objectMap(node.get("bizConfig")));
        merge(config, objectMap(node.get("config")));
        copyKnown(node, config);
        return config;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 objectMap 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
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
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param source source 参数，用于 merge 流程中的校验、计算或对象转换。
     */
    private void merge(Map<String, Object> target, Map<String, Object> source) {
        target.putAll(source);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 copyKnown 流程中的校验、计算或对象转换。
     * @param source source 参数，用于 copyKnown 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 copyKnown 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 copyKnown 流程中的校验、计算或对象转换。
     */
    private void copyKnown(Map<String, Object> source, Map<String, Object> target) {
        for (String field : KNOWN_CONFIG_FIELDS) {
            if (source.containsKey(field)) {
                target.put(field, source.get(field));
            }
        }
    }

    /**
     * 执行 previewContext 流程，围绕 preview context 完成校验、计算或结果组装。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 previewContext 流程生成的业务结果。
     */
    private Map<String, Object> previewContext(MessagePreviewReq req) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> context = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (req.context() != null) {
            context.putAll(req.context());
        }
        if (req.userId() != null) {
            context.putIfAbsent("userId", req.userId());
        }
        if (req.canvasId() != null) {
            context.putIfAbsent("canvasId", req.canvasId());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return context;
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param raw raw 参数，用于 resolveVariables 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 resolveVariables 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param warnings warnings 参数，用于 resolveVariables 流程中的校验、计算或对象转换。
     * @return 返回 resolveVariables 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveVariables(Object raw, Map<String, Object> context, List<String> warnings) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        ((Map<Object, Object>) map).forEach((key, value) -> {
            if (key != null) {
                variables.put(String.valueOf(key), resolveAny(value, context, warnings));
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return variables;
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param String string 参数，用于 resolveAny 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param warnings warnings 参数，用于 resolveAny 流程中的校验、计算或对象转换。
     * @return 返回 resolveAny 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Object resolveAny(Object value, Map<String, Object> context, List<String> warnings) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof String text) {
            return resolveText(text, context, warnings);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            ((Map<Object, Object>) map).forEach((key, item) -> {
                if (key != null) {
                    resolved.put(String.valueOf(key), resolveAny(item, context, warnings));
                }
            });
            return resolved;
        }
        if (value instanceof List<?> list) {
            List<Object> resolved = new ArrayList<>(list.size());
            for (Object item : list) {
                resolved.add(resolveAny(item, context, warnings));
            }
            return resolved;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param text text 参数，用于 resolveText 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 resolveText 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param warnings warnings 参数，用于 resolveText 流程中的校验、计算或对象转换。
     * @return 返回 resolveText 流程生成的业务结果。
     */
    private Object resolveText(String text, Map<String, Object> context, List<String> warnings) {
        String exact = exactVariableName(text);
        if (exact != null) {
            Object value = contextValue(context, exact);
            if (value == null) {
                addMissing(warnings, exact);
                return text;
            }
            // Exact variable references preserve the original value type for channel payload previews.
            return value;
        }
        String withHandlebars = replaceTokens(text, HANDLEBARS_TOKEN, context, warnings);
        return replaceTokens(withHandlebars, DOLLAR_TOKEN, context, warnings);
    }

    /**
     * 执行 exactVariableName 流程，围绕 exact variable name 完成校验、计算或结果组装。
     *
     * @param text text 参数，用于 exactVariableName 流程中的校验、计算或对象转换。
     * @return 返回 exact variable name 生成的文本或业务键。
     */
    private String exactVariableName(String text) {
        Matcher handlebars = HANDLEBARS_TOKEN.matcher(text);
        if (handlebars.matches()) {
            return handlebars.group(1);
        }
        Matcher dollar = DOLLAR_TOKEN.matcher(text);
        if (dollar.matches()) {
            return dollar.group(1);
        }
        return null;
    }

    /**
     * 执行 replaceTokens 流程，围绕 replace tokens 完成校验、计算或结果组装。
     *
     * @param text text 参数，用于 replaceTokens 流程中的校验、计算或对象转换。
     * @param pattern pattern 参数，用于 replaceTokens 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 replaceTokens 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param warnings warnings 参数，用于 replaceTokens 流程中的校验、计算或对象转换。
     * @return 返回 replace tokens 生成的文本或业务键。
     */
    private String replaceTokens(String text, Pattern pattern, Map<String, Object> context, List<String> warnings) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String field = matcher.group(1);
            Object value = contextValue(context, field);
            if (value == null) {
                addMissing(warnings, field);
                // Leave unresolved tokens visible so authors can see which personalization field is missing.
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 执行 contextValue 流程，围绕 context value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 contextValue 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 contextValue 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Object contextValue(Map<String, Object> context, String field) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (context.containsKey(field)) {
            return context.get(field);
        }
        Object current = context;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String part : field.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(part);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return current;
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param warnings warnings 参数，用于 addMissing 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void addMissing(List<String> warnings, String field) {
        warnings.add("MISSING_VARIABLE:" + field);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param String string 参数，用于 maskedMap 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> maskedMap(Map<String, Object> value) {
        Object masked = DataMaskingUtil.maskObject(value, PREVIEW_SENSITIVE_KEYS);
        return masked instanceof Map<?, ?> ? (Map<String, Object>) masked : Map.of();
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 string 流程中的校验、计算或对象转换。
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param fallback fallback 参数，用于 string 流程中的校验、计算或对象转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }
}
