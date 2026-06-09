package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseIncidentDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_incident")
public class CdpWarehouseIncidentDO {

    /** CDP数仓事故主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓事故事故业务键 */
    private String incidentKey;

    /** CDP数仓事故来源类型 */
    private String sourceType;

    /** CDP数仓事故来源 ID */
    private Long sourceId;

    /** CDP数仓事故严重级别 */
    private String severity;

    /** CDP数仓事故当前状态 */
    private String status;

    /** CDP数仓事故标题 */
    private String title;

    /** CDP数仓事故说明 */
    private String description;

    /** CDP数仓事故发生数量 */
    private Long occurrenceCount;

    /** CDP数仓事故首次可见时间 */
    private LocalDateTime firstSeenAt;

    /** CDP数仓事故最近可见时间 */
    private LocalDateTime lastSeenAt;

    /** CDP数仓事故确认人 */
    private String acknowledgedBy;

    /** CDP数仓事故确认时间 */
    private LocalDateTime acknowledgedAt;

    /** CDP数仓事故解决人 */
    private String resolvedBy;

    /** CDP数仓事故解决时间 */
    private LocalDateTime resolvedAt;

    /** CDP数仓事故创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓事故最后更新时间 */
    private LocalDateTime updatedAt;
}
