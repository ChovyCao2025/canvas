package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpComputedTagDependencyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_computed_tag_dependency")
public class CdpComputedTagDependencyDO {
    /** CDP计算标签依赖主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** CDP计算标签依赖标签编码 */
    private String tagCode;
    /** CDP计算标签依赖依赖的上游标签编码 */
    private String dependsOnTagCode;
    /** CDP计算标签依赖创建时间 */
    private LocalDateTime createdAt;
}
