package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CreatorCollaborationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("creator_collaboration")
public class CreatorCollaborationDO {

    /** 达人协作主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 关联的达人 ID */
    private Long creatorId;

    /** 达人协作权益类型 */
    private String offerType;

    /** 达人协作固定费用金额 */
    private BigDecimal fixedFeeAmount;

    /** 达人协作佣金速率 */
    private BigDecimal commissionRate;

    /** 达人协作跟踪链接 */
    private String trackingLink;

    /** 达人协作折扣编码 */
    private String discountCode;

    /** 达人协作当前状态 */
    private String status;

    /** 达人协作权限扩展元数据明细 JSON */
    private String permissionsMetadataJson;

    /** 达人协作扩展元数据 JSON */
    private String metadataJson;

    /** 达人协作创建人 */
    private String createdBy;

    /** 达人协作创建时间 */
    private LocalDateTime createdAt;

    /** 达人协作最后更新时间 */
    private LocalDateTime updatedAt;
}
