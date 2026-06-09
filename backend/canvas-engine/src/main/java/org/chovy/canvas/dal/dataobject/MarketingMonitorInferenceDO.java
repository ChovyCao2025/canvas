package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MarketingMonitorInferenceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_inference")
public class MarketingMonitorInferenceDO {

    /** 营销监控推理主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的事项 ID */
    private Long itemId;

    /** 营销监控推理来源 ID */
    private Long sourceId;

    /** 关联的服务商 ID */
    private Long providerId;

    /** 关联的模板 ID */
    private Long templateId;

    /** 营销监控推理模型标识 */
    private String modelKey;

    /** 营销监控推理MODELVERSION */
    private String modelVersion;

    /** 营销监控推理服务商状态 */
    private String providerStatus;

    /** 营销监控推理是否使用兜底回复 */
    private Boolean fallbackUsed;

    /** 营销监控推理INPUTHASH */
    private String inputHash;

    /** 营销监控推理PROMPTHASH */
    private String promptHash;

    /** 营销监控推理情绪标签 */
    private String sentimentLabel;

    /** 营销监控推理情绪评分 */
    private BigDecimal sentimentScore;

    /** 营销监控推理置信度 */
    private BigDecimal confidence;

    /** 营销监控推理实体识别 JSON */
    private String entitiesJson;

    /** 营销监控推理主题识别 JSON */
    private String topicsJson;

    /** 营销监控推理风险标记 JSON */
    private String riskFlagsJson;

    /** 营销监控推理证据明细 JSON */
    private String evidenceJson;

    /** 营销监控推理LATENCYMS */
    private Long latencyMs;

    /** 营销监控推理请求人 */
    private String requestedBy;

    /** 营销监控推理创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控推理最后更新时间 */
    private LocalDateTime updatedAt;
}
