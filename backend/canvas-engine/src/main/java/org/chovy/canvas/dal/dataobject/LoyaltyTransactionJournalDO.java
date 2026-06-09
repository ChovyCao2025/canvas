package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LoyaltyTransactionJournalDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("loyalty_transaction_journal")
public class LoyaltyTransactionJournalDO {

    /** 会员忠诚度交易流水主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的用户 ID */
    private String userId;

    /** 关联的账户 ID */
    private Long accountId;

    /** 会员忠诚度交易流水交易业务键 */
    private String transactionKey;

    /** 会员忠诚度交易流水交易类型 */
    private String transactionType;

    /** 会员忠诚度交易流水积分变动值 */
    private Integer pointsDelta;

    /** 会员忠诚度交易流水积分类型 */
    private String pointsType;

    /** 会员忠诚度交易流水变动后积分余额 */
    private Integer balanceAfter;

    /** 会员忠诚度交易流水来源类型 */
    private String sourceType;

    /** 会员忠诚度交易流水来源 ID */
    private String sourceId;

    /** 会员忠诚度交易流水原因说明 */
    private String reason;

    /** 会员忠诚度交易流水发生时间 */
    private LocalDateTime occurredAt;

    /** 会员忠诚度交易流水过期时间 */
    private LocalDateTime expiresAt;

    /** 会员忠诚度交易流水创建时间 */
    private LocalDateTime createdAt;
}
