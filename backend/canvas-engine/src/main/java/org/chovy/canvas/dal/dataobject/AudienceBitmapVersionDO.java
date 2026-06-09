package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AudienceBitmapVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("audience_bitmap_version")
public class AudienceBitmapVersionDO {

    /** 人群位图版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的人群 ID */
    private Long audienceId;

    /** 人群位图版本版本号 */
    private Long version;

    /** 人群位图版本位图业务键 */
    private String bitmapKey;

    /** 人群位图版本预估大小 */
    private Long estimatedSize;

    /** 人群位图版本位图大小KB */
    private Long bitmapSizeKb;

    /** 人群位图版本来源 */
    private String source;

    /** 人群位图版本当前状态 */
    private String status;

    /** 人群位图版本创建人 */
    private String createdBy;

    /** 人群位图版本创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 人群位图版本就绪时间 */
    private LocalDateTime readyAt;
}
