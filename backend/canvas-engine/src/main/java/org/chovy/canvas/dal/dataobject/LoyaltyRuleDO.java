package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("loyalty_rule")
public class LoyaltyRuleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ruleKey;

    private String ruleType;

    private Integer pointsDelta;

    private String rewardKey;

    private Integer pointsCost;

    private String benefitKey;

    private String benefitName;

    private String minTierCode;

    private String configJson;

    private Integer enabled;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
