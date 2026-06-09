package org.chovy.canvas.engine.audience;

/**
 * Audience Compute 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param audienceId 人群定义 ID.
 * @param audienceName 人群展示名称.
 * @param status 计算状态，如 READY、FAILED 或 IN_PROGRESS.
 * @param estimatedSize 估算命中人数.
 * @param bitmapSizeKb 序列化后位图大小，单位 KB.
 * @param errorMsg 失败或进行中提示信息.
 */
public record AudienceComputeResult(
        Long audienceId,
        String audienceName,
        String status,
        Long estimatedSize,
        Integer bitmapSizeKb,
        String errorMsg
) {
    /**
     * 查询或读取 ready 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param audienceId audienceId 对应的业务主键或标识
     * @param audienceName audienceName 方法执行所需的业务参数
     * @param estimatedSize estimatedSize 数量、阈值或分页参数
     * @param bitmapSizeKb bitmapSizeKb 数量、阈值或分页参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static AudienceComputeResult ready(Long audienceId, String audienceName, Long estimatedSize, Integer bitmapSizeKb) {
        return new AudienceComputeResult(audienceId, audienceName, "READY", estimatedSize, bitmapSizeKb, null);
    }

    /**
     * 执行 failed 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param audienceId audienceId 对应的业务主键或标识
     * @param audienceName audienceName 方法执行所需的业务参数
     * @param errorMsg errorMsg 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static AudienceComputeResult failed(Long audienceId, String audienceName, String errorMsg) {
        return new AudienceComputeResult(audienceId, audienceName, "FAILED", null, null, errorMsg);
    }

    /**
     * 构造“已有计算进行中”的结果。
     *
     * <p>调用方据此等待或重试，不会重复启动同一人群的离线计算。
     *
     * @param audienceId 人群定义 ID
     * @param audienceName 人群展示名
     * @param message 进行中原因或锁占用提示
     * @return IN_PROGRESS 状态的人群计算结果
     */
    public static AudienceComputeResult inProgress(Long audienceId, String audienceName, String message) {
        return new AudienceComputeResult(audienceId, audienceName, "IN_PROGRESS", null, null, message);
    }

    /**
     * 执行 success 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean success() {
        return "READY".equals(status);
    }

    /**
     * 执行 in Progress 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean inProgress() {
        return "IN_PROGRESS".equals(status);
    }
}
