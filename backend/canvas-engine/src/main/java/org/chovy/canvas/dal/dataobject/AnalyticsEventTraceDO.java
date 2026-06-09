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
 * AnalyticsEventTraceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("analytics_event_trace")
public class AnalyticsEventTraceDO {

    /** 分析事件轨迹主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的执行 ID */
    private String executionId;

    /** 关联的画布 ID */
    private Long canvasId;

    /** 关联的用户 ID */
    private String userId;

    /** 分析事件轨迹事件编码 */
    private String eventCode;

    /** 关联的节点 ID */
    private String nodeId;

    /** 分析事件轨迹节点类型 */
    private String nodeType;

    /** 分析事件轨迹节点名称 */
    private String nodeName;

    /** 分析事件轨迹当前状态 */
    private String status;

    /** 分析事件轨迹输入明细 JSON */
    private String inputJson;

    /** 分析事件轨迹输出明细 JSON */
    private String outputJson;

    /** 分析事件轨迹错误信息 */
    private String errorMessage;

    /** 分析事件轨迹开始时间 */
    private LocalDateTime startedAt;

    /** 分析事件轨迹结束时间 */
    private LocalDateTime finishedAt;

    /** 分析事件轨迹耗时毫秒数 */
    private Long durationMs;

    /** 分析事件轨迹事件发生时间 */
    private LocalDateTime eventTime;

    /** 分析事件轨迹接收时间 */
    private LocalDateTime receivedAt;

    /** 分析事件轨迹结构版本 */
    private Integer schemaVersion;

    /** 分析事件轨迹业务值 */
    private BigDecimal businessValue;

    /** 分析事件轨迹留存分类 */
    private String retentionClass;

    /** 分析事件轨迹归档状态 */
    private String archiveStatus;

    /** 分析事件轨迹归档时间 */
    private LocalDateTime archivedAt;

    /** 分析事件轨迹法务保留 */
    private Boolean legalHold;

    /** 分析事件轨迹创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
