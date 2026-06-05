package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_user_prediction_snapshot")
public class AiUserPredictionSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long runId;
    private String userId;
    private String modelKey;
    private String modelVersion;
    private BigDecimal churnProbability;
    private String churnRiskBand;
    private Integer bestSendHour;
    private BigDecimal confidence;
    private String featureJson;
    private String contributionJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
