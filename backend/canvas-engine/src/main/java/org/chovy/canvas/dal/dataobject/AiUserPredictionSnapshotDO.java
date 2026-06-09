package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiUserPredictionSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ai_user_prediction_snapshot")
public class AiUserPredictionSnapshotDO {

    /** AI用户预测快照主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的运行 ID */
    private Long runId;
    /** 关联的用户 ID */
    private String userId;
    /** AI用户预测快照模型标识 */
    private String modelKey;
    /** AI用户预测快照模型版本 */
    private String modelVersion;
    /** AI用户预测快照流失概率 */
    private BigDecimal churnProbability;
    /** AI用户预测快照流失风险分层 */
    private String churnRiskBand;
    /** AI用户预测快照最佳发送小时 */
    private Integer bestSendHour;
    /** AI用户预测快照置信度 */
    private BigDecimal confidence;
    /** AI用户预测快照模型特征 JSON */
    private String featureJson;
    /** AI用户预测快照贡献明细 JSON */
    private String contributionJson;

    /** AI用户预测快照创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
