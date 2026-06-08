package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiBigScreenDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_big_screen")
public class BiBigScreenDO {

    /** BI大大屏主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI大大屏大屏业务键 */
    private String screenKey;

    /** BI大大屏名称 */
    private String name;

    /** BI大大屏说明 */
    private String description;

    /** BI大大屏大小明细 JSON */
    private String sizeJson;

    /** BI大大屏背景配置 JSON */
    private String backgroundJson;

    /** BI大大屏布局明细 JSON */
    private String layoutJson;

    /** BI大大屏刷新明细 JSON */
    private String refreshJson;

    /** BI大大屏移动端布局明细 JSON */
    private String mobileLayoutJson;

    /** BI大大屏当前状态 */
    private String status;

    /** BI大大屏版本号 */
    private Integer version;

    /** BI大大屏创建人 */
    private String createdBy;

    /** BI大大屏创建时间 */
    private LocalDateTime createdAt;

    /** BI大大屏最后更新时间 */
    private LocalDateTime updatedAt;
}
