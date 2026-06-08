package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDatasetExtractRefreshRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dataset_extract_refresh_run")
public class BiDatasetExtractRefreshRunDO {

    /** BI数据集抽取刷新运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** BI数据集抽取刷新运行数据集业务键 */
    private String datasetKey;
    /** BI数据集抽取刷新运行当前状态 */
    private String status;
    /** BI数据集抽取刷新运行数量 */
    private Long rowCount;
    /** BI数据集抽取刷新运行耗时毫秒数 */
    private Long durationMs;
    /** BI数据集抽取刷新运行物化表 */
    private String materializedTable;
    /** BI数据集抽取刷新运行留存状态 */
    private String retentionStatus;
    /** BI数据集抽取刷新运行请求人 */
    private String requestedBy;
    /** BI数据集抽取刷新运行开始时间 */
    private LocalDateTime startedAt;
    /** BI数据集抽取刷新运行结束时间 */
    private LocalDateTime finishedAt;
    /** BI数据集抽取刷新运行抽取表清理时间 */
    private LocalDateTime droppedAt;
    /** BI数据集抽取刷新运行错误信息 */
    private String errorMessage;
    /** BI数据集抽取刷新运行创建时间 */
    private LocalDateTime createdAt;
    /** BI数据集抽取刷新运行最后更新时间 */
    private LocalDateTime updatedAt;
}
