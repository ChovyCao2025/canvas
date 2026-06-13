package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控名单条目实体，保存单个主体在名单中的哈希值、展示掩码和生效窗口。
 */
@Data
@TableName("risk_list_entry")
public class RiskListEntryDO {

    /**
     * 名单条目的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 条目所属租户。
     */
    private Long tenantId;
    /**
     * 条目所在名单的业务键。
     */
    private String listKey;
    /**
     * 主体原始值的哈希，运行时用该值匹配且不落库原始标识。
     */
    private String subjectHash;
    /**
     * 脱敏后的主体展示值，供运营和审计界面识别条目。
     */
    private String subjectMasked;
    /**
     * 主体进入名单的业务原因。
     */
    private String reason;
    /**
     * 条目来源，例如上游系统、批量导入或人工操作。
     */
    private String source;
    /**
     * 条目开始生效时间；为空时表示创建后即可生效。
     */
    private LocalDateTime effectiveFrom;
    /**
     * 条目过期时间；过期后运行时不再命中。
     */
    private LocalDateTime expiresAt;
    /**
     * 创建或导入该条目的操作人。
     */
    private String createdBy;
    /**
     * 条目需要审批时关联的审批记录 ID。
     */
    private Long approvalId;
    /**
     * 条目创建时间。
     */
    private LocalDateTime createdAt;
}
