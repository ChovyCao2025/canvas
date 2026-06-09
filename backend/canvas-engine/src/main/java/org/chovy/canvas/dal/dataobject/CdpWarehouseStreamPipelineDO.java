package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseStreamPipelineDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_stream_pipeline")
public class CdpWarehouseStreamPipelineDO {

    /** CDP数仓流管道主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓流管道管道业务键 */
    private String pipelineKey;

    /** CDP数仓流管道展示名称 */
    private String displayName;

    /** CDP数仓流管道来源类型 */
    private String sourceType;

    /** CDP数仓流管道来源引用 */
    private String sourceRef;

    /** CDP数仓流管道来源Topic */
    private String sourceTopic;

    /** CDP数仓流管道消费方分组 */
    private String consumerGroup;

    /** CDP数仓流管道处理器类型 */
    private String processorType;

    /** CDP数仓流管道落地类型 */
    private String sinkType;

    /** CDP数仓流管道落地引用 */
    private String sinkRef;

    /** CDP数仓流管道投递语义 */
    private String deliverySemantics;

    /** CDP数仓流管道检查点间隔秒数 */
    private Integer checkpointIntervalSeconds;

    /** CDP数仓流管道最大延迟毫秒 */
    private Long maxLagMs;

    /** CDP数仓流管道最大检查点年龄秒数 */
    private Integer maxCheckpointAgeSeconds;

    /** CDP数仓流管道生命周期状态 */
    private String lifecycleStatus;

    /** CDP数仓流管道负责人姓名 */
    private String ownerName;

    /** CDP数仓流管道配置 JSON */
    private String configJson;

    /** 关联的最近检查点 ID */
    private String lastCheckpointId;

    /** CDP数仓流管道最近来源偏移 */
    private String lastSourceOffset;

    /** CDP数仓流管道最近提交偏移 */
    private String lastCommittedOffset;

    /** CDP数仓流管道最近水位时间 */
    private LocalDateTime lastWatermarkTime;

    /** CDP数仓流管道最近检查点时间 */
    private LocalDateTime lastCheckpointAt;

    /** CDP数仓流管道最近延迟毫秒 */
    private Long lastLagMs;

    /** CDP数仓流管道最近运行时状态 */
    private String lastRuntimeStatus;

    /** CDP数仓流管道最近状态消息 */
    private String lastStatusMessage;

    /** CDP数仓流管道最近上报人 */
    private String lastReportedBy;

    /** CDP数仓流管道创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓流管道最后更新时间 */
    private LocalDateTime updatedAt;
}
