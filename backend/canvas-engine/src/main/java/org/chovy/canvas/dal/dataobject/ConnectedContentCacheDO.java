package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConnectedContentCacheDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("connected_content_cache")
public class ConnectedContentCacheDO {

    /** 联网内容CONTENTCACHE主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 联网内容CONTENTCACHE缓存键 */
    private String cacheKey;

    /** 联网内容CONTENTCACHEURL 哈希 */
    private String urlHash;

    /** 联网内容CONTENTCACHE请求内容哈希 */
    private String requestHash;

    /** 联网内容CONTENTCACHE响应内容 JSON */
    private String responseJson;

    /** 联网内容CONTENTCACHE当前状态 */
    private String status;

    /** 联网内容CONTENTCACHE过期时间 */
    private LocalDateTime expiresAt;

    /** 联网内容CONTENTCACHE创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 联网内容CONTENTCACHE最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
