package org.chovy.canvas.common.enums;

/**
 * `SCHEDULED_TRIGGER` 节点支持的调度方式。
 *
 * <p>用于解释定时触发节点应该按“周期”还是“单次”方式调度。
 * 该值通常来自节点 `bizConfig.scheduleType`。
 */
public final class ScheduleType {

    /** 周期触发（例如 cron 表达式）。 */
    public static final String CRON = "CRON";

    /** 单次触发（指定一个确定触发时刻）。 */
    public static final String ONCE = "ONCE";

    /**
     * 构造 ScheduleType 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
// 未来若扩展固定间隔触发，可在此新增常量并同步前端选项。
    // 常量值需与前端配置面板枚举保持一致。
    private ScheduleType() {}
}
