package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_computed_profile_run")
public class CdpComputedProfileRunDO {
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String DUPLICATED = "DUPLICATED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long attrId;
    private String sourceEventId;
    private String status;
    private Long scannedCount;
    private Long matchedCount;
    private Long changedCount;
    private Long unchangedCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
