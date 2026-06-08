package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiEmbedTokenDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_embed_token")
public class BiEmbedTokenDO {

    /** BI嵌入令牌主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** BI嵌入令牌令牌哈希 */
    private String tokenHash;

    /** BI嵌入令牌资源类型 */
    private String resourceType;

    /** BI嵌入令牌资源 ID */
    private Long resourceId;

    /** BI嵌入令牌资源业务键 */
    private String resourceKey;

    /** 关联的用户 ID */
    private String userId;

    /** BI嵌入令牌范围明细 JSON */
    private String scopeJson;

    /** BI嵌入令牌一次性随机串 */
    private String nonce;

    /** BI嵌入令牌过期时间 */
    private LocalDateTime expiresAt;

    /** BI嵌入令牌吊销 */
    private Boolean revoked;

    /** BI嵌入令牌消费时间 */
    private LocalDateTime consumedAt;

    /** BI嵌入令牌消费来源 */
    private String consumedOrigin;

    /** BI嵌入令牌访问次数 */
    private Integer accessCount;

    /** BI嵌入令牌最大访问次数 */
    private Integer maxAccessCount;

    /** BI嵌入令牌每分钟访问限制 */
    private Integer rateLimitPerMinute;

    /** BI嵌入令牌速率窗口开始时间 */
    private LocalDateTime rateWindowStartedAt;

    /** BI嵌入令牌限流窗口内访问次数 */
    private Integer rateWindowCount;

    /** BI嵌入令牌最近访问时间 */
    private LocalDateTime lastAccessedAt;

    /** BI嵌入令牌最近访问来源 */
    private String lastAccessOrigin;

    /** BI嵌入令牌创建时间 */
    private LocalDateTime createdAt;
}
