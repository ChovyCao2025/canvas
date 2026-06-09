package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AnalyticsExportJobDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("analytics_export_job")
public class AnalyticsExportJobDO {

    /** 分析导出任务主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 分析导出任务报表类型 */
    private String reportType;

    /** 分析导出任务查询条件 JSON */
    private String queryJson;

    /** 分析导出任务导出行数上限 */
    private Integer rowLimit;

    /** 分析导出任务当前状态 */
    private String status;

    /** 分析导出任务文件访问地址 */
    private String fileUrl;

    /** 分析导出任务错误信息 */
    private String errorMessage;

    /** 分析导出任务创建人 */
    private String createdBy;

    /** 分析导出任务创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 分析导出任务最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
