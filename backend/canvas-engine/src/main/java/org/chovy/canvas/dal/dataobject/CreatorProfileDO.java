package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CreatorProfileDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("creator_profile")
public class CreatorProfileDO {

    /** 达人画像主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 达人画像服务商 */
    private String provider;

    /** 达人画像处理 */
    private String handle;

    /** 达人画像处理业务键 */
    private String handleKey;

    /** 达人画像展示名称 */
    private String displayName;

    /** 达人画像达人层级 */
    private String creatorTier;

    /** 达人画像主渠道 */
    private String primaryChannel;

    /** CREATOR画像FOLLOWERCOUNT数量 */
    private Long followerCount;

    /** 达人画像平均互动速率 */
    private BigDecimal avgEngagementRate;

    /** 达人画像标签列表 JSON */
    private String tagsJson;

    /** 达人画像当前状态 */
    private String status;

    /** 达人画像风险状态 */
    private String riskStatus;

    /** 达人画像扩展元数据 JSON */
    private String metadataJson;

    /** 达人画像创建人 */
    private String createdBy;

    /** 达人画像创建时间 */
    private LocalDateTime createdAt;

    /** 达人画像最后更新时间 */
    private LocalDateTime updatedAt;
}
