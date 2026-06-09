package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LoyaltyMemberAccountDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("loyalty_member_account")
public class LoyaltyMemberAccountDO {

    /** 会员忠诚度成员账户主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的用户 ID */
    private String userId;

    /** 会员忠诚度成员账户成员编号 */
    private String memberNo;

    /** 会员忠诚度成员账户层级编码 */
    private String tierCode;

    /** 会员忠诚度成员账户积分余额 */
    private Integer pointsBalance;

    /** 会员忠诚度成员账户生命周期积分 */
    private Integer lifetimePoints;

    /** 会员忠诚度成员账户当前状态 */
    private String status;

    /** 会员忠诚度成员账户入会时间 */
    private LocalDateTime enrolledAt;

    /** 会员忠诚度成员账户创建时间 */
    private LocalDateTime createdAt;

    /** 会员忠诚度成员账户最后更新时间 */
    private LocalDateTime updatedAt;
}
