package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ProgrammaticDspMutationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("programmatic_dsp_mutation")
public class ProgrammaticDspMutationDO {

    /** 程序化DSP变更主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的席位 ID */
    private Long seatId;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 关联的行事项 ID */
    private Long lineItemId;

    /** 关联的供给路径 ID */
    private Long supplyPathId;

    /** 程序化DSP变更服务商 */
    private String provider;

    /** 程序化DSP变更变更业务键 */
    private String mutationKey;

    /** 程序化DSP变更变更类型 */
    private String mutationType;

    /** 程序化DSP变更实体类型 */
    private String entityType;

    /** 关联的外部实体 ID */
    private String externalEntityId;

    /** 程序化DSP变更请求内容哈希 */
    private String requestHash;

    /** 程序化DSP变更幂等键 */
    private String idempotencyKey;

    /** 程序化DSP变更当前状态 */
    private String status;

    /** 程序化DSP变更审批状态 */
    private String approvalStatus;

    /** 程序化DSP变更空跑运行要求 */
    private Integer dryRunRequired;

    /** 程序化DSP变更载荷 JSON */
    private String payloadJson;

    /** 程序化DSP变更校验结果 JSON */
    private String validationJson;

    /** 程序化DSP变更服务商请求报文 JSON */
    private String providerRequestJson;

    /** 程序化DSP变更服务商响应报文 JSON */
    private String providerResponseJson;

    /** 程序化DSP变更错误码 */
    private String errorCode;

    /** 程序化DSP变更错误信息 */
    private String errorMessage;

    /** 程序化DSP变更创建人 */
    private String createdBy;

    /** 程序化DSP变更批准人 */
    private String approvedBy;

    /** 程序化DSP变更批准时间 */
    private LocalDateTime approvedAt;

    /** 程序化DSP变更执行人 */
    private String executedBy;

    /** 程序化DSP变更执行时间 */
    private LocalDateTime executedAt;

    /** 程序化DSP变更创建时间 */
    private LocalDateTime createdAt;

    /** 程序化DSP变更最后更新时间 */
    private LocalDateTime updatedAt;
}
