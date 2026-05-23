package org.chovy.canvas.engine.audience;

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

    public boolean success() {
        return "READY".equals(status);
    }
}
