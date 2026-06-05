package org.chovy.canvas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 更新画布草稿请求。
 */
@Data
public class CanvasUpdateReq {

    /** 画布名称。 */
    @Size(max = 128)
    private String name;

    /** 画布描述。 */
    @Size(max = 1024)
    private String description;

    /** 最新草稿 graph JSON */
    @Size(max = 1_000_000)
    private String graphJson;

    /** 更新人（用户名或操作人标识）。 */
    @Size(max = 128)
    private String updatedBy;

    /** 触发类型：REALTIME | SCHEDULED。 */
    @Pattern(regexp = "REALTIME|SCHEDULED")
    private String triggerType;

    /** 定时触发表达式（triggerType=SCHEDULED 时使用）。 */
    @Size(max = 128)
    private String cronExpression;

    /** 平铺项目分组 key。 */
    @Size(max = 128)
    private String projectKey;

    /** 平铺项目展示名。 */
    @Size(max = 255)
    private String projectName;

    /** 平铺文件夹分组 key。 */
    @Size(max = 128)
    private String folderKey;

    /** 平铺文件夹展示名。 */
    @Size(max = 255)
    private String folderName;

    /** 生效开始时间（可选）。 */
    private LocalDateTime validStart;

    /** 生效结束时间（可选）。 */
    private LocalDateTime validEnd;

    /** 全局最大触发次数限制（可选）。 */
    @Min(1)
    private Integer maxTotalExecutions;

    /** 单用户每日触发上限（可选）。 */
    @Min(1)
    private Integer perUserDailyLimit;

    /** 单用户总触发上限（可选）。 */
    @Min(1)
    private Integer perUserTotalLimit;

    /** 单用户冷却时间（秒，可选）。 */
    @Min(0)
    private Integer cooldownSeconds;

    /** 控制组比例，0~50；0 表示不开启。 */
    @Min(0)
    @Max(50)
    private Integer controlGroupPercent;

    /** 控制组分桶盐值。 */
    @Size(max = 64)
    private String controlGroupSalt;

    /** 转化事件编码。 */
    @Size(max = 128)
    private String conversionEventCode;

    /** 归因窗口天数。 */
    @Min(1)
    private Integer attributionWindowDays;
}
