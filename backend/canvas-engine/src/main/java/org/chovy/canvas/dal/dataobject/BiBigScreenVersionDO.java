package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiBigScreenVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_big_screen_version")
public class BiBigScreenVersionDO {

    /** BI大大屏版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的大屏 ID */
    private Long screenId;

    /** BI大大屏版本大屏业务键 */
    private String screenKey;

    /** BI大大屏版本版本号 */
    private Integer version;

    /** BI大大屏版本当前状态 */
    private String status;

    /** BI大大屏版本资源内容 JSON */
    private String resourceJson;

    /** BI大大屏版本发布人 */
    private String publishedBy;

    /** BI大大屏版本创建时间 */
    private LocalDateTime createdAt;
}
