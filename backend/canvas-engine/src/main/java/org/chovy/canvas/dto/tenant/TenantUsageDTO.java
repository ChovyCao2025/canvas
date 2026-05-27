package org.chovy.canvas.dto.tenant;

import lombok.Data;

@Data
public class TenantUsageDTO {

    private long tenantId;
    private long canvasCount;
    private long publishedCanvasCount;
    private long executionCount;
    private long failedExecutionCount;
    private long dlqCount;
}
