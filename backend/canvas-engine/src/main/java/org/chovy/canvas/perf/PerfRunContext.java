package org.chovy.canvas.perf;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 性能压测运行上下文工具。
 *
 * <p>负责从触发 payload 或执行上下文中提取 perfRunId，并为压测链路提供统一的上下文标识传递方式。
 * <p>该类不参与业务决策，只用于把压测批次和执行记录、指标统计关联起来。
 */
public final class PerfRunContext {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_:-]{1,80}");

    private PerfRunContext() {
    }

    public static String extract(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object raw = payload.get("perfRunId");
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank() || !SAFE_ID.matcher(value).matches()) {
            return null;
        }
        return value;
    }
}
