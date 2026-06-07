package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_embed_token")
public class BiEmbedTokenDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String tokenHash;

    private String resourceType;

    private Long resourceId;

    private String resourceKey;

    private String userId;

    private String scopeJson;

    private String nonce;

    private LocalDateTime expiresAt;

    private Boolean revoked;

    private LocalDateTime consumedAt;

    private String consumedOrigin;

    private Integer accessCount;

    private Integer maxAccessCount;

    private Integer rateLimitPerMinute;

    private LocalDateTime rateWindowStartedAt;

    private Integer rateWindowCount;

    private LocalDateTime lastAccessedAt;

    private String lastAccessOrigin;

    private LocalDateTime createdAt;
}
