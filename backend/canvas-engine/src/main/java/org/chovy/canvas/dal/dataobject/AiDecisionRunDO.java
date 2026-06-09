package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AiDecisionRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ai_decision_run")
public class AiDecisionRunDO {

    /** AI决策运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** AI决策运行模型标识 */
    private String modelKey;
    /** AI决策运行模型版本 */
    private String modelVersion;
    /** AI决策运行决策范围 */
    private String decisionScope;
    /** AI决策运行运行日期 */
    private LocalDate runDate;
    /** AI决策运行当前状态 */
    private String status;
    /** AI决策运行请求处理数量 */
    private Integer requestedCount;
    /** AI决策运行已处理数量 */
    private Integer processedCount;
    /** AI决策运行已跳过数量 */
    private Integer skippedCount;
    /** AI决策运行处理失败数量 */
    private Integer failedCount;
    /** AI决策运行扩展元数据 JSON */
    private String metadataJson;
    /** AI决策运行创建人 */
    private String createdBy;
    /** AI决策运行开始时间 */
    private LocalDateTime startedAt;
    /** AI决策运行结束时间 */
    private LocalDateTime finishedAt;
    /** AI决策运行错误信息 */
    private String errorMessage;
    /** AI决策运行创建时间 */
    private LocalDateTime createdAt;
    /** AI决策运行最后更新时间 */
    private LocalDateTime updatedAt;
}
