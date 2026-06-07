package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_contact_profile")
public class ConversationContactProfileDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String userId;

    private String displayName;

    private String externalContactId;

    private String privateDomainSource;

    private String owner;

    private String lifecycleStage;

    private String tagsJson;

    private String attributesJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
