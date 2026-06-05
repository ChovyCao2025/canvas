package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_e2e_certification_run")
public class CdpWarehouseE2eCertificationRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    private String status;

    private String mode;

    @TableField("require_physical")
    private Integer requirePhysical;

    @TableField("require_realtime")
    private Integer requireRealtime;

    @TableField("require_data_path_proof")
    private Integer requireDataPathProof;

    @TableField("window_start")
    private LocalDateTime windowStart;

    @TableField("window_end")
    private LocalDateTime windowEnd;

    @TableField("contract_keys_json")
    private String contractKeysJson;

    @TableField("evidence_json")
    private String evidenceJson;

    @TableField("production_readiness_json")
    private String productionReadinessJson;

    @TableField("live_table_inspection_json")
    private String liveTableInspectionJson;

    @TableField("realtime_pipeline_status_json")
    private String realtimePipelineStatusJson;

    @TableField("realtime_job_status_json")
    private String realtimeJobStatusJson;

    @TableField("data_path_proof_json")
    private String dataPathProofJson;

    @TableField("error_message")
    private String errorMessage;

    @TableField("requested_by")
    private String requestedBy;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
