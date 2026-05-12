package org.chovy.canvas.common;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * PII 数据脱敏工具（设计文档 13.8节）。
 * 在写入 execution_trace 和日志前调用，保护用户隐私。
 */
public final class DataMaskingUtil {

    private DataMaskingUtil() {}

    // 手机号：保留前3后4，中间用 **** 替换
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\d{3})\\d{4}(\\d{4})");

    // 身份证：保留前3后4
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("(\\d{3})\\d{11}(\\w{4})");

    /** 脱敏手机号：138****8888 */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    /** 脱敏身份证：110***********1234 */
    public static String maskIdCard(String id) {
        if (id == null) return null;
        return ID_CARD_PATTERN.matcher(id).replaceAll("$1***********$2");
    }

    /** 对 JSON 字符串中的敏感字段值进行脱敏（按字段名模糊匹配）*/
    public static String maskJson(String json, Set<String> sensitiveKeys) {
        if (json == null || json.isBlank()) return json;
        String result = json;
        for (String key : sensitiveKeys) {
            // 替换 "phone":"13812345678" → "phone":"138****5678"
            result = result.replaceAll(
                    "\"" + key + "\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{4})\"",
                    "\"" + key + "\":\"$1****$2\""
            );
            // 替换身份证
            result = result.replaceAll(
                    "\"" + key + "\"\\s*:\\s*\"(\\d{3})\\d{11}(\\w{4})\"",
                    "\"" + key + "\":\"$1***********$2\""
            );
        }
        return result;
    }

    /** 默认敏感字段集合 */
    public static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
            "phone", "mobile", "phoneNumber", "mobileNumber",
            "idCard", "idNumber", "identityCard",
            "bankCard", "cardNumber"
    );
}
