package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiExportJobDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_export_job")
public class BiExportJobDO {

    /** BI导出任务主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI导出任务资源类型 */
    private String resourceType;

    /** BI导出任务资源 ID */
    private Long resourceId;

    /** BI导出任务导出格式 */
    private String exportFormat;

    /** BI导出任务请求明细 JSON */
    private String requestJson;

    /** BI导出任务导出行数上限 */
    private Integer rowLimit;

    /** BI导出任务当前状态 */
    private String status;

    /** BI导出任务进度百分比比例 */
    private Integer progressPercent;

    /** BI导出任务文件访问地址 */
    private String fileUrl;

    /** BI导出任务存储服务商 */
    private String storageProvider;

    /** BI导出任务存储对象键 */
    private String storageKey;

    /** BI导出任务保留天数 */
    private Integer retentionDays;

    /** BI导出任务过期时间 */
    private LocalDateTime expiresAt;

    /** BI导出任务下载数量 */
    private Integer downloadCount;

    /** BI导出任务最近下载时间 */
    private LocalDateTime lastDownloadedAt;

    /** BI导出任务审批状态 */
    private String approvalStatus;

    /** BI导出任务审批原因 */
    private String approvalReason;

    /** BI导出任务请求人 */
    private String requestedBy;

    /** BI导出任务请求时间 */
    private LocalDateTime requestedAt;

    /** BI导出任务审核人 */
    private String reviewedBy;

    /** BI导出任务审核时间 */
    private LocalDateTime reviewedAt;

    /** BI导出任务评审备注 */
    private String reviewComment;

    /** BI导出任务错误信息 */
    private String errorMessage;

    /** BI导出任务已重试次数 */
    private Integer retryCount;

    /** BI导出任务最大重试次数 */
    private Integer maxRetryCount;

    /** BI导出任务下次重试时间 */
    private LocalDateTime nextRetryAt;

    /** BI导出任务最近重试时间 */
    private LocalDateTime lastRetryAt;

    /** BI导出任务重试耗尽时间 */
    private LocalDateTime retryExhaustedAt;

    /** BI导出任务创建人 */
    private String createdBy;

    /** BI导出任务创建时间 */
    private LocalDateTime createdAt;

    /** BI导出任务最后更新时间 */
    private LocalDateTime updatedAt;
}
