package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PaidMediaAudienceDestinationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("paid_media_audience_destination")
public class PaidMediaAudienceDestinationDO {

    /** 付费媒体人群目标端主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 付费媒体人群目标端服务商 */
    private String provider;

    /** 付费媒体人群目标端目标端业务键 */
    private String destinationKey;

    /** 付费媒体人群目标端展示名称 */
    private String displayName;

    /** 关联的账户 ID */
    private String accountId;

    /** 关联的外部人群 ID */
    private String externalAudienceId;

    /** 付费媒体人群目标端标识类型明细 JSON */
    private String identifierTypesJson;

    /** 付费媒体人群目标端同意渠道 */
    private String consentChannel;

    /** 付费媒体人群目标端强制同意 */
    private Integer enforceConsent;

    /** 付费媒体人群目标端是否启用 */
    private Integer enabled;

    /** 付费媒体人群目标端扩展元数据 JSON */
    private String metadataJson;

    /** 付费媒体人群目标端创建人 */
    private String createdBy;

    /** 付费媒体人群目标端创建时间 */
    private LocalDateTime createdAt;

    /** 付费媒体人群目标端最后更新时间 */
    private LocalDateTime updatedAt;
}
