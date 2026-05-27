package org.chovy.canvas.engine.request;

/**
 * 画布执行请求状态常量。
 *
 * <p>集中定义执行请求从待处理、处理中到完成、失败、重试等状态值，避免在 SQL、服务和控制器中散落魔法字符串。
 * <p>状态值需要与数据库记录、管理端筛选和积压指标统计保持一致。
 */
public final class CanvasExecutionRequestStatus {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String RETRY = "RETRY";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";

    private CanvasExecutionRequestStatus() {
    }
}
