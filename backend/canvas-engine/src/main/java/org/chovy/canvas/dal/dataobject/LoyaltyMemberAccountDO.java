package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("loyalty_member_account")
public class LoyaltyMemberAccountDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String userId;

    private String memberNo;

    private String tierCode;

    private Integer pointsBalance;

    private Integer lifetimePoints;

    private String status;

    private LocalDateTime enrolledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
