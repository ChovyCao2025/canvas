package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_privacy_audience_rebuild_automation_run")
public class CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO {

    /** CDP数仓隐私人群重建自动化运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓隐私人群重建自动化运行触发来源 */
    private String triggerSource;

    /** CDP数仓隐私人群重建自动化运行当前状态 */
    private String status;

    /** CDP数仓隐私人群重建自动化运行ACTOR */
    private String actor;

    /** CDP数仓隐私人群重建自动化运行扫描上限 */
    private Integer scanLimit;

    /** CDP数仓隐私人群重建自动化运行人群上限 */
    private Integer audienceLimit;

    /** CDP数仓隐私人群重建自动化运行是否重试失败记录 */
    private Integer retryFailed;

    /** CDP数仓隐私人群重建自动化运行SCANNED */
    private Integer scanned;

    /** CDP数仓隐私人群重建自动化运行ELIGIBLE */
    private Integer eligible;

    /** CDP数仓隐私人群重建自动化运行TRIGGERED */
    private Integer triggered;

    /** CDP数仓隐私人群重建自动化运行SKIPPED */
    private Integer skipped;

    /** CDP数仓隐私人群重建自动化运行FAILED */
    private Integer failed;

    /** CDP数仓隐私人群重建自动化运行运行结果 JSON */
    private String resultJson;

    /** CDP数仓隐私人群重建自动化运行错误信息 */
    private String errorMessage;

    /** CDP数仓隐私人群重建自动化运行开始时间 */
    private LocalDateTime startedAt;

    /** CDP数仓隐私人群重建自动化运行结束时间 */
    private LocalDateTime finishedAt;

    /** CDP数仓隐私人群重建自动化运行创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓隐私人群重建自动化运行最后更新时间 */
    private LocalDateTime updatedAt;
}
