package org.chovy.canvas.engine.audience;

/**
 * Audience Compute 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record AudienceComputeResult(
        Long audienceId,
        String audienceName,
        String status,
        Long estimatedSize,
        Integer bitmapSizeKb,
        String errorMsg
) {
    public static AudienceComputeResult ready(Long audienceId, String audienceName, Long estimatedSize, Integer bitmapSizeKb) {
        return new AudienceComputeResult(audienceId, audienceName, "READY", estimatedSize, bitmapSizeKb, null);
    }

    public static AudienceComputeResult failed(Long audienceId, String audienceName, String errorMsg) {
        return new AudienceComputeResult(audienceId, audienceName, "FAILED", null, null, errorMsg);
    }

    public static AudienceComputeResult inProgress(Long audienceId, String audienceName, String message) {
        return new AudienceComputeResult(audienceId, audienceName, "IN_PROGRESS", null, null, message);
    }

    public boolean success() {
        return "READY".equals(status);
    }

    public boolean inProgress() {
        return "IN_PROGRESS".equals(status);
    }
}
