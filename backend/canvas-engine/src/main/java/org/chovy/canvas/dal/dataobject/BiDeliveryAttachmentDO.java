package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDeliveryAttachmentDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_delivery_attachment")
public class BiDeliveryAttachmentDO {

    /** BI投递附件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI投递附件任务类型 */
    private String jobType;

    /** 关联的任务 ID */
    private Long jobId;

    /** BI投递附件任务业务键 */
    private String jobKey;

    /** 关联的投递日志 ID */
    private Long deliveryLogId;

    /** BI投递附件资源类型 */
    private String resourceType;

    /** BI投递附件资源 ID */
    private Long resourceId;

    /** BI投递附件附件业务键 */
    private String attachmentKey;

    /** BI投递附件附件类型 */
    private String attachmentType;

    /** BI投递附件文件名 */
    private String fileName;

    /** BI投递附件内容类型 */
    private String contentType;

    /** BI投递附件文件路径 */
    private String filePath;

    /** BI投递附件文件访问地址 */
    private String fileUrl;

    /** BI投递附件存储服务商 */
    private String storageProvider;

    /** BI投递附件存储对象键 */
    private String storageKey;

    /** BI投递附件文件大小字节数 */
    private Long sizeBytes;

    /** BI投递附件保留天数 */
    private Integer retentionDays;

    /** BI投递附件过期时间 */
    private LocalDateTime expiresAt;

    /** BI投递附件下载数量 */
    private Integer downloadCount;

    /** BI投递附件最近下载时间 */
    private LocalDateTime lastDownloadedAt;

    /** BI投递附件当前状态 */
    private String status;

    /** BI投递附件错误信息 */
    private String errorMessage;

    /** BI投递附件创建人 */
    private String createdBy;

    /** BI投递附件创建时间 */
    private LocalDateTime createdAt;

    /** BI投递附件最后更新时间 */
    private LocalDateTime updatedAt;
}
