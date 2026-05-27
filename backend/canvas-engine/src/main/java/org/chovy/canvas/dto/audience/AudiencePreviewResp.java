package org.chovy.canvas.dto.audience;

import java.util.List;

public record AudiencePreviewResp(
        long estimatedSize,
        List<String> sampleUserIds
) {}
