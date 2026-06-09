package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LoyaltyRuleDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("loyalty_rule")
public class LoyaltyRuleDO {

    /** 会员忠诚度规则主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会员忠诚度规则规则业务键 */
    private String ruleKey;

    /** 会员忠诚度规则规则类型 */
    private String ruleType;

    /** 会员忠诚度规则积分增量 */
    private Integer pointsDelta;

    /** 会员忠诚度规则奖励业务键 */
    private String rewardKey;

    /** 会员忠诚度规则积分成本 */
    private Integer pointsCost;

    /** 会员忠诚度规则权益业务键 */
    private String benefitKey;

    /** 会员忠诚度规则权益名称 */
    private String benefitName;

    /** 会员忠诚度规则最小层级编码 */
    private String minTierCode;

    /** 会员忠诚度规则配置 JSON */
    private String configJson;

    /** 会员忠诚度规则是否启用 */
    private Integer enabled;

    /** 会员忠诚度规则生效开始时间 */
    private LocalDateTime startsAt;

    /** 会员忠诚度规则生效结束时间 */
    private LocalDateTime endsAt;

    /** 会员忠诚度规则创建时间 */
    private LocalDateTime createdAt;

    /** 会员忠诚度规则最后更新时间 */
    private LocalDateTime updatedAt;
}
