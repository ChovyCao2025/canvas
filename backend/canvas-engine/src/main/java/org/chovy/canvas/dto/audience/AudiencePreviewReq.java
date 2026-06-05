package org.chovy.canvas.dto.audience;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AudiencePreviewReq(
        @NotBlank
        @Size(max = 64)
        String dataSourceType,
        @NotBlank
        @Size(max = 1_000_000)
        String ruleJson,
        @Min(1)
        @Max(100)
        Integer sampleLimit
) {}
