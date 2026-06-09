package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SearchMarketingProviderChangeDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_provider_change")
public class SearchMarketingProviderChangeDO {

    /** 搜索营销服务商变更主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销服务商变更来源 ID */
    private Long sourceId;

    /** 关联的变更 ID */
    private Long mutationId;

    /** 搜索营销服务商变更服务商 */
    private String provider;

    /** 关联的外部资源 ID */
    private String externalResourceId;

    /** 搜索营销服务商变更变更类型 */
    private String changeType;

    /** 搜索营销服务商变更变更字段明细 JSON */
    private String changedFieldsJson;

    /** 搜索营销服务商变更服务商操作人 */
    private String providerActor;

    /** 搜索营销服务商变更时间 */
    private LocalDateTime providerChangedAt;

    /** 搜索营销服务商变更对账状态 */
    private String reconciliationStatus;

    /** 搜索营销服务商变更证据明细 JSON */
    private String evidenceJson;

    /** 搜索营销服务商变更创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销服务商变更最后更新时间 */
    private LocalDateTime updatedAt;
}
