package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("ai_provider")
public class AiProviderDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String providerKey;

    private String displayName;

    private String providerType;

    private String endpoint;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String encryptedApiKey;

    private String defaultModel;

    private String defaultParams;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
