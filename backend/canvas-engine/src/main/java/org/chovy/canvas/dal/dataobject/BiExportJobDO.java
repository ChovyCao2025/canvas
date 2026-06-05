package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_export_job")
public class BiExportJobDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String resourceType;

    private Long resourceId;

    private String exportFormat;

    private String requestJson;

    private Integer rowLimit;

    private String status;

    private String fileUrl;

    private String storageProvider;

    private String storageKey;

    private Integer retentionDays;

    private LocalDateTime expiresAt;

    private Integer downloadCount;

    private LocalDateTime lastDownloadedAt;

    private String approvalStatus;

    private String approvalReason;

    private String requestedBy;

    private LocalDateTime requestedAt;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewComment;

    private String errorMessage;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
