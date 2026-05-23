package org.chovy.canvas.engine.audience;

public record AudienceComputeResult(
        Long audienceId,
        String audienceName,
        boolean ready,
        Long estimatedSize,
        Integer bitmapSizeKb,
        String errorMsg
) {

    public static AudienceComputeResult ready(Long audienceId, String audienceName, Long estimatedSize, Integer bitmapSizeKb) {
        return new AudienceComputeResult(audienceId, audienceName, true, estimatedSize, bitmapSizeKb, null);
    }

    public static AudienceComputeResult failed(Long audienceId, String audienceName, String errorMsg) {
        return new AudienceComputeResult(audienceId, audienceName, false, null, null, errorMsg);
    }
}
