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

    /** 允许向执行链路透传的 perfRunId 安全格式。 */
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_:-]{1,80}");

    /**
     * 构造 PerfRunContext 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private PerfRunContext() {
    }

    /**
     * 执行 extract 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param payload payload 请求体、消息体或事件载荷
     * @return 转换或查询得到的字符串结果
     */
    public static String extract(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        // 只接受安全字符集，避免把任意外部输入当成压测批次号继续向下传播。
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
