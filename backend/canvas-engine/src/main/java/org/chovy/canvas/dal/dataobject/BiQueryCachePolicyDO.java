package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_query_cache_policy")
public class BiQueryCachePolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String resourceType;

    private String resourceKey;

    private Boolean enabled;

    private Long ttlSeconds;

    private String cacheMode;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
