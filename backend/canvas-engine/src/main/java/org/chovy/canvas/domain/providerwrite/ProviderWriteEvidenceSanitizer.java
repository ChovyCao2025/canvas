package org.chovy.canvas.domain.providerwrite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ProviderWriteEvidenceSanitizer 编排 domain.providerwrite 场景的领域业务规则。
 */
public final class ProviderWriteEvidenceSanitizer {

    public static final String REDACTED = "[REDACTED]";

    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "accesstoken",
            "refreshtoken",
            "clientsecret",
            "developertoken",
            "apikey",
            "password",
            "secret",
            "authorization",
            "credential",
            "credentials",
            "soapheader",
            "soapheaders");

    /**
     * 执行 ProviderWriteEvidenceSanitizer 流程，围绕 provider write evidence sanitizer 完成校验、计算或结果组装。
     */
    private ProviderWriteEvidenceSanitizer() {
    }

    /**
     * sanitizeMap 处理 domain.providerwrite 场景的业务逻辑。
     * @param values values 参数，用于 sanitizeMap 流程中的校验、计算或对象转换。
     * @return 返回 sanitizeMap 流程生成的业务结果。
     */
    public static Map<String, Object> sanitizeMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        values.forEach((key, value) -> sanitized.put(key, sanitizeValue(key, value)));
        return sanitized;
    }

    /**
     * sanitize 处理 domain.providerwrite 场景的业务逻辑。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sanitize 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    public static Object sanitize(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            map.forEach((key, nestedValue) -> sanitized.put(String.valueOf(key), sanitizeValue(key, nestedValue)));
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new ArrayList<>();
            iterable.forEach(item -> sanitized.add(sanitize(item)));
            return List.copyOf(sanitized);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 执行 sanitizeValue 流程，围绕 sanitize value 完成校验、计算或结果组装。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sanitizeValue 流程生成的业务结果。
     */
    private static Object sanitizeValue(Object key, Object value) {
        if (isSecretKey(key)) {
            return REDACTED;
        }
        return sanitize(value);
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回布尔判断结果。
     */
    private static boolean isSecretKey(Object key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toString()
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        return SECRET_KEYS.contains(normalized);
    }
}
