package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_marketing_provider_change")
public class SearchMarketingProviderChangeDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private Long mutationId;

    private String provider;

    private String externalResourceId;

    private String changeType;

    private String changedFieldsJson;

    private String providerActor;

    private LocalDateTime providerChangedAt;

    private String reconciliationStatus;

    private String evidenceJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
