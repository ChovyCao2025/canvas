package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpComputedTagRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_computed_tag_run")
public class CdpComputedTagRunDO {
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    /** CDP计算标签运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** CDP计算标签运行标签编码 */
    private String tagCode;
    /** CDP计算标签运行当前状态 */
    private String status;
    /** CDP计算标签运行循环依赖路径 */
    private String cyclePath;
    /** CDP计算标签运行扫描数量 */
    private Long scannedCount;
    /** CDP计算标签运行匹配数量 */
    private Long matchedCount;
    /** CDP计算标签运行更新数量 */
    private Long updatedCount;
    /** CDP计算标签运行已跳过数量 */
    private Long skippedCount;
    /** CDP计算标签运行处理失败数量 */
    private Long failedCount;
    /** CDP计算标签运行错误信息 */
    private String errorMessage;
    /** CDP计算标签运行开始时间 */
    private LocalDateTime startedAt;
    /** CDP计算标签运行结束时间 */
    private LocalDateTime finishedAt;
}
