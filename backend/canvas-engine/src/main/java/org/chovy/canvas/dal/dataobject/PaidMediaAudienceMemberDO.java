package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PaidMediaAudienceMemberDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("paid_media_audience_member")
public class PaidMediaAudienceMemberDO {

    /** 付费媒体人群成员主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的运行 ID */
    private Long runId;

    /** 关联的目标端 ID */
    private Long destinationId;

    /** 关联的人群 ID */
    private Long audienceId;

    /** 付费媒体人群成员服务商 */
    private String provider;

    /** 关联的用户 ID */
    private String userId;

    /** 付费媒体人群成员标识类型 */
    private String identifierType;

    /** 付费媒体人群成员标识哈希 */
    private String identifierHash;

    /** 付费媒体人群成员资格状态 */
    private String eligibilityStatus;

    /** 付费媒体人群成员原因说明 */
    private String reason;

    /** 付费媒体人群成员同步时间 */
    private LocalDateTime syncedAt;

    /** 付费媒体人群成员创建时间 */
    private LocalDateTime createdAt;

    /** 付费媒体人群成员最后更新时间 */
    private LocalDateTime updatedAt;
}
