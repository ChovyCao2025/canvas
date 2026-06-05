package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 画布主表（canvas）。
 *
 * <p>一个画布代表一个完整的工作流定义，包含触发条件、执行约束和版本管理信息。
 * 画布本身只存储元数据，具体的节点图结构存储在 {@link CanvasVersionDO} 中。
 */
@Data
@TableName("canvas")
public class CanvasDO {

    @TableId(type = IdType.AUTO)
    /** 画布主键 ID */
    private Long id;

    /** 所属租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 画布名称 */
    private String name;

    /** 画布描述 */
    private String description;

    /**
     * 画布状态，见 {@link org.chovy.canvas.common.enums.CanvasStatusEnum}。
     * DRAFT=0, PUBLISHED=1, OFFLINE=2, ARCHIVED=3, KILLED=4
     */
    private Integer status;

    /** 当前正式发布版本的版本 ID（对应 canvas_version.id） */
    private Long publishedVersionId;

    /** 创建人 */
    private String createdBy;

    /** 1=官方示例画布，受 canvas.examples.enabled 控制展示 */
    private Integer isExample;

    /** 来源官方模板 key，用于启动导入幂等判断 */
    private String sourceTemplateKey;

    /** 平铺项目分组 key，用于列表过滤和轻量归类。 */
    private String projectKey;

    /** 平铺项目展示名。 */
    private String projectName;

    /** 平铺文件夹分组 key。 */
    private String folderKey;

    /** 平铺文件夹展示名。 */
    private String folderName;

    // ── 执行约束（V3）────────────────────────────────────────────────

    /** 画布有效期开始时间，null 表示不限制 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime validStart;

    /** 画布有效期结束时间，null 表示不限制 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime validEnd;

    /** 单用户全生命周期最大执行次数，null 表示不限制 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer perUserTotalLimit;

    /** 单用户每日最大执行次数，null 表示不限制 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer perUserDailyLimit;

    /** 单用户两次执行之间的冷却时间（秒），null 表示不限制 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer cooldownSeconds;

    /** 控制组比例，0~50；0 表示不开启控制组留存 */
    private Integer controlGroupPercent;

    /** 控制组分桶盐值；为空时使用默认盐值 */
    private String controlGroupSalt;

    /** 归因转化事件编码；为空时该画布不参与转化归因 */
    private String conversionEventCode;

    /** 末次触达归因窗口天数 */
    private Integer attributionWindowDays;

    /**
     * 画布全局最大触发总次数（所有用户合计，生命周期内），null 表示不限制。
     *
     * <p>语义：总量配额，由 {@link org.chovy.canvas.engine.trigger.TriggerPreCheckService}
     * 通过 Redis INCR 原子扣减，画布下线/Kill/归档时由 {@code cleanupCanvasQuotas} 清理。
     *
     * <p>并发执行上限通过全局配置 {@code canvas.execution.max-concurrency} 控制，
     * 与本字段无关。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxTotalExecutions;

    // ── 灰度发布（V3）──────────────────────────────────────────────

    /** 灰度版本 ID，null 表示未开启灰度 */
    private Long canaryVersionId;

    /** 灰度流量比例（0~100），null 表示未开启灰度 */
    private Integer canaryPercent;

    /** 上一个正式版本 ID，用于版本回滚 */
    private Long previousVersionId;

    /** 乐观锁版本号，防止并发编辑覆盖 */
    private Integer editVersion;

    // ── 触发方式（V24）──────────────────────────────────────────────

    /**
     * 触发方式，见 {@link org.chovy.canvas.common.enums.TriggerType}。
     * 冗余字段，方便列表查询过滤；实际触发路由以 START 节点的 config.triggerType 为准。
     */
    private String triggerType;

    /** SCHEDULED 模式下的 CRON 表达式（冗余字段，实际配置在节点 config 中） */
    private String cronExpression;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
