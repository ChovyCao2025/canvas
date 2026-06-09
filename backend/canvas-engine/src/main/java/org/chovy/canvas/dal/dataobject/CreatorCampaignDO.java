package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CreatorCampaignDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("creator_campaign")
public class CreatorCampaignDO {

    /** 达人营销活动主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 达人营销活动营销活动业务键 */
    private String campaignKey;

    /** 达人营销活动营销活动名称 */
    private String campaignName;

    /** 达人营销活动目标 */
    private String objective;

    /** 达人营销活动预算金额 */
    private BigDecimal budgetAmount;

    /** 达人营销活动币种 */
    private String currency;

    /** 达人营销活动开始时间 */
    private LocalDateTime startAt;

    /** 达人营销活动结束时间 */
    private LocalDateTime endAt;

    /** 达人营销活动当前状态 */
    private String status;

    /** 达人营销活动扩展元数据 JSON */
    private String metadataJson;

    /** 达人营销活动创建人 */
    private String createdBy;

    /** 达人营销活动创建时间 */
    private LocalDateTime createdAt;

    /** 达人营销活动最后更新时间 */
    private LocalDateTime updatedAt;
}
