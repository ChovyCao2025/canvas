package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_computed_tag_run")
public class CdpComputedTagRunDO {
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String tagCode;
    private String status;
    private String cyclePath;
    private Long scannedCount;
    private Long matchedCount;
    private Long updatedCount;
    private Long skippedCount;
    private Long failedCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
