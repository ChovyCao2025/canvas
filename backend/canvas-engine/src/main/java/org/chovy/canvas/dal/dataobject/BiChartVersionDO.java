package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_chart_version")
public class BiChartVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private Long chartId;

    private String chartKey;

    private Integer version;

    private String status;

    private String resourceJson;

    private String publishedBy;

    private LocalDateTime createdAt;
}
