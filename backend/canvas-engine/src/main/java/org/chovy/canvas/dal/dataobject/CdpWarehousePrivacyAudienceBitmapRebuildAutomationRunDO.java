package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_privacy_audience_rebuild_automation_run")
public class CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String triggerSource;

    private String status;

    private String actor;

    private Integer scanLimit;

    private Integer audienceLimit;

    private Integer retryFailed;

    private Integer scanned;

    private Integer eligible;

    private Integer triggered;

    private Integer skipped;

    private Integer failed;

    private String resultJson;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
