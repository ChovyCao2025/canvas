package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dataset_version")
public class BiDatasetVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private Long datasetId;

    private String datasetKey;

    private Integer version;

    private String status;

    private String resourceJson;

    private String publishedBy;

    private LocalDateTime createdAt;
}
