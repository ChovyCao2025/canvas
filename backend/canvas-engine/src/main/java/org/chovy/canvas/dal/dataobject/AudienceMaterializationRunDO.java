package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audience_materialization_run")
public class AudienceMaterializationRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long audienceId;

    private Long version;

    private String status;

    private String ruleJson;

    private Long matchedUsers;

    private String bitmapKey;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String createdBy;
}
