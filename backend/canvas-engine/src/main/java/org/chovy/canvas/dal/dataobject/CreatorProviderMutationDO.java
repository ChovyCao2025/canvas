package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CreatorProviderMutationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("creator_provider_mutation")
public class CreatorProviderMutationDO {

    /** 达人服务商变更主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 关联的协作 ID */
    private Long collaborationId;

    /** 关联的交付物 ID */
    private Long deliverableId;

    /** 关联的达人 ID */
    private Long creatorId;

    /** 达人服务商变更服务商 */
    private String provider;

    /** 达人服务商变更变更业务键 */
    private String mutationKey;

    /** 达人服务商变更变更类型 */
    private String mutationType;

    /** 达人服务商变更实体类型 */
    private String entityType;

    /** 关联的外部实体 ID */
    private String externalEntityId;

    /** 达人服务商变更请求内容哈希 */
    private String requestHash;

    /** 达人服务商变更幂等键 */
    private String idempotencyKey;

    /** 达人服务商变更当前状态 */
    private String status;

    /** 达人服务商变更审批状态 */
    private String approvalStatus;

    /** 达人服务商变更空跑运行要求 */
    private Integer dryRunRequired;

    /** 达人服务商变更载荷 JSON */
    private String payloadJson;

    /** 达人服务商变更校验结果 JSON */
    private String validationJson;

    /** 达人服务商变更服务商请求报文 JSON */
    private String providerRequestJson;

    /** 达人服务商变更服务商响应报文 JSON */
    private String providerResponseJson;

    /** 达人服务商变更错误码 */
    private String errorCode;

    /** 达人服务商变更错误信息 */
    private String errorMessage;

    /** 达人服务商变更创建人 */
    private String createdBy;

    /** 达人服务商变更批准人 */
    private String approvedBy;

    /** 达人服务商变更批准时间 */
    private LocalDateTime approvedAt;

    /** 达人服务商变更执行人 */
    private String executedBy;

    /** 达人服务商变更执行时间 */
    private LocalDateTime executedAt;

    /** 达人服务商变更创建时间 */
    private LocalDateTime createdAt;

    /** 达人服务商变更最后更新时间 */
    private LocalDateTime updatedAt;
}
