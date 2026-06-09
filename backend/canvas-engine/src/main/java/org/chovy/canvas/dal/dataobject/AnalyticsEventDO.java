package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AnalyticsEventDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("analytics_event")
public class AnalyticsEventDO {

    /** 分析事件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 分析事件事件编码 */
    private String eventCode;

    /** 关联的用户 ID */
    private String userId;

    /** 关联的匿名 ID */
    private String anonymousId;

    /** 关联的会话 ID */
    private String sessionId;

    /** 分析事件平台 */
    private String platform;

    /** 分析事件设备类型 */
    private String deviceType;

    /** 分析事件来源 */
    private String source;

    /** 分析事件事件发生时间 */
    private LocalDateTime eventTime;

    /** 分析事件接收时间 */
    private LocalDateTime receivedAt;

    /** 分析事件结构版本 */
    private Integer schemaVersion;

    /** 分析事件业务值 */
    private BigDecimal businessValue;

    /** 分析事件事件属性 JSON */
    private String attributesJson;

    /** 分析事件留存分类 */
    private String retentionClass;

    /** 分析事件归档状态 */
    private String archiveStatus;

    /** 分析事件归档时间 */
    private LocalDateTime archivedAt;

    /** 分析事件法务保留 */
    private Boolean legalHold;

    /** 分析事件创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
