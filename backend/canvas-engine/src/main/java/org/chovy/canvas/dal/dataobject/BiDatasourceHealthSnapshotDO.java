package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_datasource_health_snapshot")
public class BiDatasourceHealthSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceKey;

    private String sourceType;

    private Boolean available;

    private String message;

    private LocalDateTime checkedAt;

    private LocalDateTime createdAt;
}
