package org.chovy.canvas.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 更新画布草稿请求。
 */
@Data
public class CanvasUpdateReq {

    /** 画布名称。 */
    private String name;

    /** 画布描述。 */
    private String description;

    /** 最新草稿 graph JSON */
    private String graphJson;

    /** 更新人（用户名或操作人标识）。 */
    private String updatedBy;

    /** 触发类型：REALTIME | SCHEDULED。 */
    private String triggerType;

    /** 定时触发表达式（triggerType=SCHEDULED 时使用）。 */
    private String cronExpression;

    /** 生效开始时间（可选）。 */
    private LocalDateTime validStart;

    /** 生效结束时间（可选）。 */
    private LocalDateTime validEnd;

    /** 全局最大触发次数限制（可选）。 */
    private Integer maxTotalExecutions;

    /** 单用户每日触发上限（可选）。 */
    private Integer perUserDailyLimit;

    /** 单用户总触发上限（可选）。 */
    private Integer perUserTotalLimit;

    /** 单用户冷却时间（秒，可选）。 */
    private Integer cooldownSeconds;
}
