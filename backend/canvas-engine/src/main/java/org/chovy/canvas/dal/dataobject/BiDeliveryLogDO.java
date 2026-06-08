package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BiDeliveryLogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_delivery_log")
public class BiDeliveryLogDO {

    /** BI投递日志主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI投递日志任务类型 */
    private String jobType;

    /** 关联的任务 ID */
    private Long jobId;

    /** BI投递日志任务业务键 */
    private String jobKey;

    /** BI投递日志资源类型 */
    private String resourceType;

    /** BI投递日志资源 ID */
    private Long resourceId;

    /** BI投递日志触达渠道 */
    private String channel;

    /** BI投递日志接收方配置 JSON */
    private String receiverJson;

    /** BI投递日志载荷 JSON */
    private String payloadJson;

    /** BI投递日志指标值 */
    private BigDecimal metricValue;

    /** BI投递日志当前状态 */
    private String status;

    /** BI投递日志消息 */
    private String message;

    /** BI投递日志错误信息 */
    private String errorMessage;

    /** BI投递日志已重试次数 */
    private Integer retryCount;

    /** BI投递日志最大重试次数 */
    private Integer maxRetryCount;

    /** BI投递日志下次重试时间 */
    private LocalDateTime nextRetryAt;

    /** BI投递日志最近重试时间 */
    private LocalDateTime lastRetryAt;

    /** BI投递日志重试耗尽时间 */
    private LocalDateTime retryExhaustedAt;

    /** BI投递日志触发人 */
    private String triggeredBy;

    /** BI投递日志创建时间 */
    private LocalDateTime createdAt;

    /** BI投递日志最后更新时间 */
    private LocalDateTime updatedAt;
}
