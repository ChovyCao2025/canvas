package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiQueryGovernancePolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_query_governance_policy")
public class BiQueryGovernancePolicyDO {

    /** BI查询治理策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** BI查询治理策略数据集业务键 */
    private String datasetKey;

    /** BI查询治理策略超时时间毫秒 */
    private Long timeoutMs;

    /** BI查询治理策略配额行数 */
    private Integer quotaRows;

    /** BI查询治理策略最后更新人 */
    private String updatedBy;

    /** BI查询治理策略创建时间 */
    private LocalDateTime createdAt;

    /** BI查询治理策略最后更新时间 */
    private LocalDateTime updatedAt;
}
