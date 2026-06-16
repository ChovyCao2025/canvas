package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控名单实体，描述租户下可被规则和名单匹配器引用的一张治理名单。
 */
@Data
@TableName("risk_list")
public class RiskListDO {

    /**
     * 名单记录的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 名单所属租户。
     */
    private Long tenantId;

    /**
     * 名单业务键，规则 DSL 和运行时匹配器通过该键引用名单。
     */
    private String listKey;

    /**
     * 名单类型，例如黑名单、白名单、灰名单、观察名单或合规黑名单。
     */
    private String listType;

    /**
     * 名单条目允许写入和匹配的主体标识类型。
     */
    private String subjectType;

    /**
     * 名单生命周期状态。
     */
    private String status;

    /**
     * 条目变更是否需要审批后才能生效。
     */
    private Boolean requiresApproval;

    /**
     * 名单负责人或系统归属方。
     */
    private String owner;

    /**
     * 名单创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 名单元数据最后更新时间。
     */
    private LocalDateTime updatedAt;
}
