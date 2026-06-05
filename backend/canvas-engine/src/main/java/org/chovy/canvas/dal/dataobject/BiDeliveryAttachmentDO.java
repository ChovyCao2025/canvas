package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_delivery_attachment")
public class BiDeliveryAttachmentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String jobType;

    private Long jobId;

    private String jobKey;

    private Long deliveryLogId;

    private String resourceType;

    private Long resourceId;

    private String attachmentKey;

    private String attachmentType;

    private String fileName;

    private String contentType;

    private String filePath;

    private String fileUrl;

    private String storageProvider;

    private String storageKey;

    private Long sizeBytes;

    private Integer retentionDays;

    private LocalDateTime expiresAt;

    private Integer downloadCount;

    private LocalDateTime lastDownloadedAt;

    private String status;

    private String errorMessage;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
