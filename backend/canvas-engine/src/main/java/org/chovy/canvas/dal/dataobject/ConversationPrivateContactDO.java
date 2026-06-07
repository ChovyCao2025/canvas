package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_private_contact")
public class ConversationPrivateContactDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String provider;

    private String externalContactId;

    private String userId;

    private String displayName;

    private String avatarUrl;

    private String corpName;

    private String gender;

    private String unionIdHash;

    private String tagsJson;

    private String attributesJson;

    private String rawPayloadJson;

    private LocalDateTime syncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
