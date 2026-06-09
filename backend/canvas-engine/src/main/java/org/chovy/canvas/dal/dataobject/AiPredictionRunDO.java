package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AiPredictionRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ai_prediction_run")
public class AiPredictionRunDO {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    /** AI预测运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** AI预测运行模型标识 */
    private String modelKey;
    /** AI预测运行模型版本 */
    private String modelVersion;
    /** AI预测运行运行日期 */
    private LocalDate runDate;
    /** AI预测运行当前状态 */
    private String status;
    /** AI预测运行已处理数量 */
    private Integer processedCount;
    /** AI预测运行已跳过数量 */
    private Integer skippedCount;
    /** AI预测运行处理失败数量 */
    private Integer failedCount;
    /** AI预测运行开始时间 */
    private LocalDateTime startedAt;
    /** AI预测运行结束时间 */
    private LocalDateTime finishedAt;
    /** AI预测运行错误信息 */
    private String errorMessage;
}
