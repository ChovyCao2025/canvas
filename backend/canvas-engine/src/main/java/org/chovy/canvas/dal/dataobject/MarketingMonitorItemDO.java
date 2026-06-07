package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_item")
public class MarketingMonitorItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private String externalItemId;

    private String sourceType;

    private String sourceUrl;

    private String authorKey;

    private String brandKey;

    private String textContent;

    private String language;

    private LocalDateTime publishedAt;

    private LocalDateTime ingestedAt;

    private String rawPayloadJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
