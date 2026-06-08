package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiQueryCachePolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_query_cache_policy")
public class BiQueryCachePolicyDO {

    /** BI查询缓存策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** BI查询缓存策略资源类型 */
    private String resourceType;

    /** BI查询缓存策略资源业务键 */
    private String resourceKey;

    /** BI查询缓存策略是否启用 */
    private Boolean enabled;

    /** BI查询缓存策略缓存 TTL 秒数 */
    private Long ttlSeconds;

    /** BI查询缓存策略缓存模式 */
    private String cacheMode;

    /** BI查询缓存策略最后更新人 */
    private String updatedBy;

    /** BI查询缓存策略创建时间 */
    private LocalDateTime createdAt;

    /** BI查询缓存策略最后更新时间 */
    private LocalDateTime updatedAt;
}
