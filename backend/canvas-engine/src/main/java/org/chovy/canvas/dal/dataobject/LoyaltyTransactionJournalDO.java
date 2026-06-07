package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("loyalty_transaction_journal")
public class LoyaltyTransactionJournalDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String userId;

    private Long accountId;

    private String transactionKey;

    private String transactionType;

    private Integer pointsDelta;

    private String pointsType;

    private Integer balanceAfter;

    private String sourceType;

    private String sourceId;

    private String reason;

    private LocalDateTime occurredAt;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
