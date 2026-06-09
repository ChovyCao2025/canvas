package org.chovy.canvas.dto.audience;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AudiencePreviewReq 承载 dto.audience 场景中的不可变数据快照。
 * @param dataSourceType dataSourceType 字段。
 * @param ruleJson ruleJson 字段。
 * @param sampleLimit sampleLimit 字段。
 */
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
