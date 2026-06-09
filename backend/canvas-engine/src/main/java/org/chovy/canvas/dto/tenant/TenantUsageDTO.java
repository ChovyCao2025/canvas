package org.chovy.canvas.dto.tenant;

import lombok.Data;

/**
 * TenantUsageDTO 承载 dto.tenant 场景的接口传输数据。
 */
@Data
public class TenantUsageDTO {

    /** 租户 ID，用于将用量指标归属到具体租户。 */
    private long tenantId;
    /** 当前租户下创建过的画布总数。 */
    private long canvasCount;
    /** 当前租户处于已发布状态的画布数量。 */
    private long publishedCanvasCount;
    /** 当前租户累计产生的画布执行记录数量。 */
    private long executionCount;
    /** 当前租户执行失败的画布执行记录数量。 */
    private long failedExecutionCount;
    /** 当前租户进入死信队列、等待补偿或人工处理的消息数量。 */
    private long dlqCount;
}
