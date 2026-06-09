package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AnalyticsRetentionRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("analytics_retention_run")
public class AnalyticsRetentionRunDO {

    /** 分析留存运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 分析留存运行记录类型 */
    private String recordKind;

    /** 分析留存运行动作 */
    private String action;

    /** 分析留存运行空跑运行 */
    private Boolean dryRun;

    /** 分析留存运行扫描数量 */
    private Long scannedCount;

    /** 分析留存运行归档数量 */
    private Long archivedCount;

    /** 分析留存运行删除数量 */
    private Long deletedCount;

    /** 分析留存运行已跳过数量 */
    private Long skippedCount;

    /** 分析留存运行处理失败数量 */
    private Long failedCount;

    /** 分析留存运行开始时间 */
    private LocalDateTime startedAt;

    /** 分析留存运行结束时间 */
    private LocalDateTime finishedAt;
}
