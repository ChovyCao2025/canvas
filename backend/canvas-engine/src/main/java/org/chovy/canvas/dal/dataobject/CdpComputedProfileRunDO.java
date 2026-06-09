package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpComputedProfileRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_computed_profile_run")
public class CdpComputedProfileRunDO {
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String DUPLICATED = "DUPLICATED";

    /** CDP计算画像运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的属性 ID */
    private Long attrId;
    /** 关联的来源事件 ID */
    private String sourceEventId;
    /** CDP计算画像运行当前状态 */
    private String status;
    /** CDP计算画像运行扫描数量 */
    private Long scannedCount;
    /** CDP计算画像运行匹配数量 */
    private Long matchedCount;
    /** CDP计算画像运行变更数量 */
    private Long changedCount;
    /** CDP计算画像运行未变化数量 */
    private Long unchangedCount;
    /** CDP计算画像运行错误信息 */
    private String errorMessage;
    /** CDP计算画像运行开始时间 */
    private LocalDateTime startedAt;
    /** CDP计算画像运行结束时间 */
    private LocalDateTime finishedAt;
}
