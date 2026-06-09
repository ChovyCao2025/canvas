package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingAssetFolderDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_asset_folder")
public class MarketingAssetFolderDO {
    /** 营销资产文件夹主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销资产文件夹文件夹业务键 */
    private String folderKey;
    /** 营销资产文件夹名称 */
    private String name;
    /** 关联的父级 ID */
    private Long parentId;
    /** 营销资产文件夹创建人 */
    private String createdBy;
    /** 营销资产文件夹创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 营销资产文件夹最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
