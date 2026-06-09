package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseE2eCertificationRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_e2e_certification_run")
public class CdpWarehouseE2eCertificationRunDO {

    /** CDP数仓端到端认证运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** CDP数仓端到端认证运行当前状态 */
    private String status;

    /** CDP数仓端到端认证运行运行模式 */
    private String mode;

    /** CDP数仓端到端认证运行是否要求物理表证明 */
    @TableField("require_physical")
    private Integer requirePhysical;

    /** CDP数仓端到端认证运行是否要求实时链路证明 */
    @TableField("require_realtime")
    private Integer requireRealtime;

    /** CDP数仓端到端认证运行是否要求数据路径证明 */
    @TableField("require_data_path_proof")
    private Integer requireDataPathProof;

    /** CDP数仓端到端认证运行窗口开始时间 */
    @TableField("window_start")
    private LocalDateTime windowStart;

    /** CDP数仓端到端认证运行窗口结束时间 */
    @TableField("window_end")
    private LocalDateTime windowEnd;

    /** CDP数仓端到端认证运行契约键列表 JSON */
    @TableField("contract_keys_json")
    private String contractKeysJson;

    /** CDP数仓端到端认证运行证据明细 JSON */
    @TableField("evidence_json")
    private String evidenceJson;

    /** CDP数仓端到端认证运行生产就绪证明 JSON */
    @TableField("production_readiness_json")
    private String productionReadinessJson;

    /** CDP数仓端到端认证运行在线表巡检 JSON */
    @TableField("live_table_inspection_json")
    private String liveTableInspectionJson;

    /** CDP数仓端到端认证运行实时管道状态 JSON */
    @TableField("realtime_pipeline_status_json")
    private String realtimePipelineStatusJson;

    /** CDP数仓端到端认证运行实时任务状态 JSON */
    @TableField("realtime_job_status_json")
    private String realtimeJobStatusJson;

    /** CDP数仓端到端认证运行数据路径证明 JSON */
    @TableField("data_path_proof_json")
    private String dataPathProofJson;

    /** CDP数仓端到端认证运行错误信息 */
    @TableField("error_message")
    private String errorMessage;

    /** CDP数仓端到端认证运行请求人 */
    @TableField("requested_by")
    private String requestedBy;

    /** CDP数仓端到端认证运行开始时间 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** CDP数仓端到端认证运行结束时间 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
