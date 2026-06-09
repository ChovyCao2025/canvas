package org.chovy.canvas.domain.paidmedia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PaidMediaAudienceDestinationView 承载 domain.paidmedia 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param destinationKey destinationKey 字段。
 * @param displayName displayName 字段。
 * @param accountId accountId 字段。
 * @param externalAudienceId externalAudienceId 字段。
 * @param identifierTypes identifierTypes 字段。
 * @param consentChannel consentChannel 字段。
 * @param enforceConsent enforceConsent 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record PaidMediaAudienceDestinationView(
        Long id,
        Long tenantId,
        String provider,
        String destinationKey,
        String displayName,
        String accountId,
        String externalAudienceId,
        List<String> identifierTypes,
        String consentChannel,
        boolean enforceConsent,
        boolean enabled,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public PaidMediaAudienceDestinationView {
        identifierTypes = identifierTypes == null ? List.of() : List.copyOf(identifierTypes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * 创建 PaidMediaAudienceDestinationView 实例并注入 domain.paidmedia 场景依赖。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 PaidMediaAudienceDestinationView 流程中的校验、计算或对象转换。
     * @param destinationKey 业务键，用于在同一租户下定位资源。
     * @param displayName 名称文本，用于展示或唯一性校验。
     * @param accountId 业务对象 ID，用于定位具体记录。
     * @param externalAudienceId 业务对象 ID，用于定位具体记录。
     * @param identifierTypes identifier types 参数，用于 PaidMediaAudienceDestinationView 流程中的校验、计算或对象转换。
     * @param consentChannel consent channel 参数，用于 PaidMediaAudienceDestinationView 流程中的校验、计算或对象转换。
     * @param enforceConsent enforce consent 参数，用于 PaidMediaAudienceDestinationView 流程中的校验、计算或对象转换。
     * @param enabled enabled 参数，用于 PaidMediaAudienceDestinationView 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 PaidMediaAudienceDestinationView 流程中的校验、计算或对象转换。
     * @param updatedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    public PaidMediaAudienceDestinationView(Long id,
                                            Long tenantId,
                                            String provider,
                                            String destinationKey,
                                            String displayName,
                                            String accountId,
                                            String externalAudienceId,
                                            List<String> identifierTypes,
                                            String consentChannel,
                                            boolean enforceConsent,
                                            boolean enabled,
                                            Map<?, ?> metadata,
                                            LocalDateTime updatedAt) {
        this(id, tenantId, provider, destinationKey, displayName, accountId, externalAudienceId, identifierTypes,
                consentChannel, enforceConsent, enabled, copyMetadata(metadata), null, updatedAt, updatedAt);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param metadata metadata 参数，用于 copyMetadata 流程中的校验、计算或对象转换。
     * @return 返回 copyMetadata 流程生成的业务结果。
     */
    private static Map<String, Object> copyMetadata(Map<?, ?> metadata) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return metadata.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> right));
    }
}
