package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model_registry")
public class AiModelRegistryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long providerId;

    private String modelKey;

    private String displayName;

    private String capability;

    private Integer contextWindow;

    private String defaultParams;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
