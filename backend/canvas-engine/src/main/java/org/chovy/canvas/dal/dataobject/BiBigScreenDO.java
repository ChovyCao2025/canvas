package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_big_screen")
public class BiBigScreenDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String screenKey;

    private String name;

    private String description;

    private String sizeJson;

    private String backgroundJson;

    private String layoutJson;

    private String refreshJson;

    private String mobileLayoutJson;

    private String status;

    private Integer version;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
