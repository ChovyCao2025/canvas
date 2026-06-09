package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseStreamCheckpointDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_stream_checkpoint")
public class CdpWarehouseStreamCheckpointDO {

    /** CDP数仓流检查点主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓流检查点管道业务键 */
    private String pipelineKey;

    /** 关联的检查点 ID */
    private String checkpointId;

    /** CDP数仓流检查点来源分区 */
    private String sourcePartition;

    /** CDP数仓流检查点来源偏移 */
    private String sourceOffset;

    /** CDP数仓流检查点提交偏移 */
    private String committedOffset;

    /** CDP数仓流检查点水位时间 */
    private LocalDateTime watermarkTime;

    /** CDP数仓流检查点检查点时间 */
    private LocalDateTime checkpointTime;

    /** CDP数仓流检查点延迟毫秒 */
    private Long lagMs;

    /** CDP数仓流检查点行数量 */
    private Long rowCount;

    /** CDP数仓流检查点当前状态 */
    private String status;

    /** CDP数仓流检查点错误信息 */
    private String errorMessage;

    /** CDP数仓流检查点上报人 */
    private String reportedBy;

    /** CDP数仓流检查点来源结构版本 */
    private String sourceSchemaVersion;

    /** CDP数仓流检查点落地结构版本 */
    private String sinkSchemaVersion;

    /** CDP数仓流检查点结构状态 */
    private String schemaStatus;

    /** CDP数仓流检查点创建时间 */
    private LocalDateTime createdAt;
}
