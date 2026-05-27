package org.chovy.canvas.engine.request;

/**
 * 画布执行请求状态常量。
 *
 * <p>集中定义执行请求从待处理、处理中到完成、失败、重试等状态值，避免在 SQL、服务和控制器中散落魔法字符串。
 * <p>状态值需要与数据库记录、管理端筛选和积压指标统计保持一致。
 */
public final class CanvasExecutionRequestStatus {

    /** 待派发执行请求状态。 */
    public static final String PENDING = "PENDING";
    /** 已抢占并正在执行的请求状态。 */
    public static final String RUNNING = "RUNNING";
    /** 等待下一次重试的请求状态。 */
    public static final String RETRY = "RETRY";
    /** 已成功完成的请求状态。 */
    public static final String SUCCEEDED = "SUCCEEDED";
    /** 已最终失败的请求状态。 */
    public static final String FAILED = "FAILED";

    /**
     * 构造 CanvasExecutionRequestStatus 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private CanvasExecutionRequestStatus() {
    }
}
