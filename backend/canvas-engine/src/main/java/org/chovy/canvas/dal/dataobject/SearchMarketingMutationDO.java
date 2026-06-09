package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SearchMarketingMutationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_mutation")
public class SearchMarketingMutationDO {

    /** 搜索营销变更主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销变更来源 ID */
    private Long sourceId;

    /** 关联的机会 ID */
    private Long opportunityId;

    /** 关联的关键词 ID */
    private Long keywordId;

    /** 搜索营销变更服务商 */
    private String provider;

    /** 搜索营销变更触达渠道 */
    private String channel;

    /** 搜索营销变更变更业务键 */
    private String mutationKey;

    /** 搜索营销变更变更类型 */
    private String mutationType;

    /** 搜索营销变更实体类型 */
    private String entityType;

    /** 关联的外部实体 ID */
    private String externalEntityId;

    /** 搜索营销变更请求内容哈希 */
    private String requestHash;

    /** 搜索营销变更幂等键 */
    private String idempotencyKey;

    /** 搜索营销变更当前状态 */
    private String status;

    /** 搜索营销变更审批状态 */
    private String approvalStatus;

    /** 搜索营销变更空跑运行要求 */
    private Integer dryRunRequired;

    /** 搜索营销变更载荷 JSON */
    private String payloadJson;

    /** 搜索营销变更校验结果 JSON */
    private String validationJson;

    /** 搜索营销变更服务商请求报文 JSON */
    private String providerRequestJson;

    /** 搜索营销变更服务商响应报文 JSON */
    private String providerResponseJson;

    /** 搜索营销变更错误码 */
    private String errorCode;

    /** 搜索营销变更错误信息 */
    private String errorMessage;

    /** 搜索营销变更创建人 */
    private String createdBy;

    /** 搜索营销变更批准人 */
    private String approvedBy;

    /** 搜索营销变更批准时间 */
    private LocalDateTime approvedAt;

    /** 搜索营销变更执行人 */
    private String executedBy;

    /** 搜索营销变更执行时间 */
    private LocalDateTime executedAt;

    /** 搜索营销变更创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销变更最后更新时间 */
    private LocalDateTime updatedAt;
}
