package org.chovy.canvas.conversation.domain;

import java.util.List;
import java.util.Locale;

/**
 * 会话上下文中字符串和键值字段的规范化工具。
 */
public final class ConversationText {

    /**
     * 禁止实例化纯静态工具类。
     */
    private ConversationText() {
    }

    /**
     * 校验必填文本并去除首尾空白。
     *
     * @param value 待校验文本
     * @param message 校验失败时使用的异常消息
     * @return 去除首尾空白后的文本
     */
    public static String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 校验必填文本并按固定 Locale 转为大写。
     *
     * @param value 待规范化文本
     * @param message 校验失败时使用的异常消息
     * @return 大写后的文本
     */
    public static String upperRequired(String value, String message) {
        return required(value, message).toUpperCase(Locale.ROOT);
    }

    /**
     * 将可选文本转为大写，空值时返回兜底值。
     *
     * @param value 待规范化文本
     * @param fallback 空值时使用的兜底值
     * @return 大写后的文本或兜底值
     */
    public static String upperOptional(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 将可选业务键规范化为小写。
     *
     * @param value 待规范化业务键
     * @return 小写业务键，空白输入返回 null
     */
    public static String optionalKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化业务键列表并去重。
     *
     * @param values 待规范化业务键列表
     * @return 去空、转小写并去重后的业务键列表
     */
    public static List<String> normalizeKeys(List<String> values) {
        if (values == null) {
            return List.of();
        }
        // 技能和团队键参与匹配，统一小写可避免大小写造成路由漏匹配。
        return values.stream()
                .map(ConversationText::optionalKey)
                .filter(value -> value != null && !value.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * 将空白文本折叠为 null。
     *
     * @param value 待处理文本
     * @return 去除首尾空白后的文本或 null
     */
    public static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
