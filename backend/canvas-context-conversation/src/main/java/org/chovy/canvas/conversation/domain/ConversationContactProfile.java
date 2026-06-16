package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会话联系人画像领域对象。
 *
 * @param id 联系人画像标识
 * @param tenantId 租户标识
 * @param userId 会话用户标识
 * @param displayName 联系人展示名称
 * @param externalContactId 外部联系人标识
 * @param privateDomainSource 私域来源
 * @param owner 联系人归属人
 * @param lifecycleStage 生命周期阶段
 * @param tags 联系人标签
 * @param attributes 联系人扩展属性
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ConversationContactProfile(
        /**
         * 联系人画像标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 会话用户标识。
         */
        String userId,
        /**
         * 联系人展示名称。
         */
        String displayName,
        /**
         * 外部联系人标识。
         */
        String externalContactId,
        /**
         * 私域来源。
         */
        String privateDomainSource,
        /**
         * 联系人归属人。
         */
        String owner,
        /**
         * 生命周期阶段。
         */
        String lifecycleStage,
        /**
         * 联系人标签。
         */
        List<String> tags,
        /**
         * 联系人扩展属性。
         */
        Map<String, Object> attributes,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt) {

    /**
     * 创建联系人画像并复制可变集合属性。
     */
    public ConversationContactProfile {
        tags = DomainMaps.copyList(tags);
        attributes = DomainMaps.copy(attributes);
    }

    /**
     * 返回替换持久化标识后的联系人画像副本。
     *
     * @param id 持久化生成的画像标识
     * @return 带新标识的联系人画像
     */
    public ConversationContactProfile withId(Long id) {
        return new ConversationContactProfile(id, tenantId, userId, displayName, externalContactId,
                privateDomainSource, owner, lifecycleStage, tags, attributes, createdAt, updatedAt);
    }
}
