package org.chovy.canvas.dto.audience;

public record AudiencePreviewReq(
        String dataSourceType,
        String ruleJson,
        Integer sampleLimit
) {}
