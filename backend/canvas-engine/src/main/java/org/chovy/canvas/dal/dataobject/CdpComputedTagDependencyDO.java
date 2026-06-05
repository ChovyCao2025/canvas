package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_computed_tag_dependency")
public class CdpComputedTagDependencyDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String tagCode;
    private String dependsOnTagCode;
    private LocalDateTime createdAt;
}
