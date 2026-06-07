package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_query_governance_policy")
public class BiQueryGovernancePolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String datasetKey;

    private Long timeoutMs;

    private Integer quotaRows;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
