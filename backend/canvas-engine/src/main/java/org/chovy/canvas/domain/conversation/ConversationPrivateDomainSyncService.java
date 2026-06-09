package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationContactProfileDO;
import org.chovy.canvas.dal.dataobject.ConversationPrivateContactDO;
import org.chovy.canvas.dal.dataobject.ConversationPrivateContactOwnerDO;
import org.chovy.canvas.dal.dataobject.ConversationPrivateGroupDO;
import org.chovy.canvas.dal.dataobject.ConversationPrivateGroupMemberDO;
import org.chovy.canvas.dal.dataobject.ConversationPrivateSyncRunDO;
import org.chovy.canvas.dal.mapper.ConversationContactProfileMapper;
import org.chovy.canvas.dal.mapper.ConversationPrivateContactMapper;
import org.chovy.canvas.dal.mapper.ConversationPrivateContactOwnerMapper;
import org.chovy.canvas.dal.mapper.ConversationPrivateGroupMapper;
import org.chovy.canvas.dal.mapper.ConversationPrivateGroupMemberMapper;
import org.chovy.canvas.dal.mapper.ConversationPrivateSyncRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ConversationPrivateDomainSyncService 编排 domain.conversation 场景的领域业务规则。
 */
@Service
public class ConversationPrivateDomainSyncService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ConversationPrivateContactMapper contactMapper;
    private final ConversationPrivateContactOwnerMapper ownerMapper;
    private final ConversationPrivateGroupMapper groupMapper;
    private final ConversationPrivateGroupMemberMapper memberMapper;
    private final ConversationPrivateSyncRunMapper syncRunMapper;
    private final ConversationContactProfileMapper profileMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 ConversationPrivateDomainSyncService 实例并注入 domain.conversation 场景依赖。
     * @param contactMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ownerMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param groupMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param memberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public ConversationPrivateDomainSyncService(ConversationPrivateContactMapper contactMapper,
                                                ConversationPrivateContactOwnerMapper ownerMapper,
                                                ConversationPrivateGroupMapper groupMapper,
                                                ConversationPrivateGroupMemberMapper memberMapper,
                                                ConversationPrivateSyncRunMapper syncRunMapper,
                                                ConversationContactProfileMapper profileMapper,
                                                ObjectMapper objectMapper) {
        this(contactMapper, ownerMapper, groupMapper, memberMapper, syncRunMapper, profileMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 ConversationPrivateDomainSyncService 流程，围绕 conversation private domain sync service 完成校验、计算或结果组装。
     *
     * @param contactMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ownerMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param groupMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param memberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param syncRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    ConversationPrivateDomainSyncService(ConversationPrivateContactMapper contactMapper,
                                         ConversationPrivateContactOwnerMapper ownerMapper,
                                         ConversationPrivateGroupMapper groupMapper,
                                         ConversationPrivateGroupMemberMapper memberMapper,
                                         ConversationPrivateSyncRunMapper syncRunMapper,
                                         ConversationContactProfileMapper profileMapper,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.contactMapper = contactMapper;
        this.ownerMapper = ownerMapper;
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
        this.syncRunMapper = syncRunMapper;
        this.profileMapper = profileMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 导入私域联系人和群聊快照。
     * 方法按租户和 Provider upsert 联系人、归属人、联系人画像、群和群成员，并写入同步运行记录；失败时记录 FAILED 运行后继续抛出异常。
     */
    public PrivateDomainSyncRunView ingestSnapshot(Long tenantId,
                                                   PrivateDomainSyncCommand command,
                                                   String actor) {
        if (command == null) {
            throw new IllegalArgumentException("private-domain sync command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeProvider(command.provider());
        String syncType = normalizeText(command.syncType(), "FULL").toUpperCase(Locale.ROOT);
        LocalDateTime startedAt = now();
        int contactCount = command.contacts().size();
        int groupCount = command.groups().size();
        int memberCount = command.groups().stream().mapToInt(group -> group.members().size()).sum();
        try {
            int contactUpserted = 0;
            for (PrivateDomainContactSnapshot contact : command.contacts()) {
                upsertContact(scopedTenantId, provider, contact, startedAt);
                contactUpserted++;
            }
            int groupUpserted = 0;
            int memberUpserted = 0;
            for (PrivateDomainGroupSnapshot group : command.groups()) {
                groupUpserted += upsertGroup(scopedTenantId, provider, group, startedAt);
                for (PrivateDomainGroupMemberSnapshot member : group.members()) {
                    upsertGroupMember(scopedTenantId, provider, group.externalGroupId(), member, startedAt);
                    memberUpserted++;
                }
            }
            return insertRun(scopedTenantId, provider, syncType, "SUCCESS", actor,
                    command.sourceCursor(), command.nextCursor(), contactCount, contactUpserted,
                    groupCount, groupUpserted, memberCount, memberUpserted, 0, null,
                    command.metadata(), startedAt, now());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            insertRun(scopedTenantId, provider, syncType, "FAILED", actor,
                    command.sourceCursor(), command.nextCursor(), contactCount, 0,
                    groupCount, 0, memberCount, 0, 1, ex.getMessage(),
                    command.metadata(), startedAt, now());
            throw ex;
        }
    }

    /**
     * 查询租户内指定 Provider 的私域联系人。
     * 可按关键词和归属员工过滤，返回联系人及其 owner 信息，结果会再次做租户和 Provider 过滤。
     */
    public List<PrivateDomainContactView> contacts(Long tenantId, PrivateDomainContactQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("private-domain contact query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeProvider(query.provider());
        int limit = boundedLimit(query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<ConversationPrivateContactDO> contacts = safeList(contactMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateContactDO>()
                        .eq(ConversationPrivateContactDO::getTenantId, scopedTenantId)
                        .eq(ConversationPrivateContactDO::getProvider, provider)
                        .like(hasText(query.keyword()), ConversationPrivateContactDO::getDisplayName, query.keyword())
                        .orderByDesc(ConversationPrivateContactDO::getSyncedAt)
                        .last("LIMIT " + limit)));
        List<ConversationPrivateContactOwnerDO> owners = safeList(ownerMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateContactOwnerDO>()
                        .eq(ConversationPrivateContactOwnerDO::getTenantId, scopedTenantId)
                        .eq(ConversationPrivateContactOwnerDO::getProvider, provider)
                        .eq(hasText(query.ownerUserId()), ConversationPrivateContactOwnerDO::getOwnerUserId, query.ownerUserId())
                        .last("LIMIT " + limit)));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        Map<String, ConversationPrivateContactOwnerDO> ownerByContact = owners.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> provider.equals(row.getProvider()))
                .collect(Collectors.toMap(
                        ConversationPrivateContactOwnerDO::getExternalContactId,
                        Function.identity(),
                        (left, right) -> left));
        return contacts.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> provider.equals(row.getProvider()))
                .filter(row -> !hasText(query.ownerUserId()) || ownerByContact.containsKey(row.getExternalContactId()))
                .limit(limit)
                .map(row -> toContactView(row, ownerByContact.get(row.getExternalContactId())))
                .toList();
    }

    /**
     * 查询租户内指定 Provider 的私域群聊。
     * 可按 ownerUserId 过滤，返回群基础信息和成员摘要，用于私域工作台展示。
     */
    public List<PrivateDomainGroupView> groups(Long tenantId, PrivateDomainGroupQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("private-domain group query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeProvider(query.provider());
        int limit = boundedLimit(query.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(groupMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateGroupDO>()
                        .eq(ConversationPrivateGroupDO::getTenantId, scopedTenantId)
                        .eq(ConversationPrivateGroupDO::getProvider, provider)
                        .eq(hasText(query.ownerUserId()), ConversationPrivateGroupDO::getOwnerUserId, query.ownerUserId())
                        .orderByDesc(ConversationPrivateGroupDO::getSyncedAt)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> provider.equals(row.getProvider()))
                .limit(limit)
                .map(row -> toGroupView(scopedTenantId, provider, row))
                .toList();
    }

    /**
     * 查询租户内私域同步运行历史。
     * 按 Provider 和创建时间倒序返回有限条记录，便于排查同步来源、游标和失败原因。
     */
    public List<PrivateDomainSyncRunView> syncRuns(Long tenantId, String provider, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedProvider = normalizeProvider(provider);
        int boundedLimit = boundedLimit(limit);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(syncRunMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateSyncRunDO>()
                        .eq(ConversationPrivateSyncRunDO::getTenantId, scopedTenantId)
                        .eq(ConversationPrivateSyncRunDO::getProvider, scopedProvider)
                        .orderByDesc(ConversationPrivateSyncRunDO::getCreatedAt)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .last("LIMIT " + boundedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> scopedProvider.equals(row.getProvider()))
                .limit(boundedLimit)
                .map(this::toRunView)
                .toList();
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 upsertContact 流程中的校验、计算或对象转换。
     * @param snapshot snapshot 参数，用于 upsertContact 流程中的校验、计算或对象转换。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void upsertContact(Long tenantId,
                               String provider,
                               PrivateDomainContactSnapshot snapshot,
                               LocalDateTime syncedAt) {
        String externalContactId = required(snapshot.externalContactId(), "externalContactId");
        String ownerUserId = required(snapshot.ownerUserId(), "ownerUserId");
        String userId = userId(provider, externalContactId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationPrivateContactDO row = contactMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateContactDO>()
                        .eq(ConversationPrivateContactDO::getTenantId, tenantId)
                        .eq(ConversationPrivateContactDO::getProvider, provider)
                        .eq(ConversationPrivateContactDO::getExternalContactId, externalContactId)
                        .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new ConversationPrivateContactDO();
            row.setTenantId(tenantId);
            row.setProvider(provider);
            row.setExternalContactId(externalContactId);
            row.setCreatedAt(syncedAt);
        }
        row.setUserId(userId);
        row.setDisplayName(snapshot.displayName());
        row.setAvatarUrl(snapshot.avatarUrl());
        row.setCorpName(snapshot.corpName());
        row.setGender(snapshot.gender());
        row.setUnionIdHash(snapshot.unionIdHash());
        row.setTagsJson(json(snapshot.tags()));
        row.setAttributesJson(json(snapshot.attributes()));
        row.setRawPayloadJson(json(snapshot.rawPayload()));
        row.setSyncedAt(syncedAt);
        row.setUpdatedAt(syncedAt);
        if (row.getId() == null) {
            contactMapper.insert(row);
        } else {
            contactMapper.updateById(row);
        }
        upsertOwner(tenantId, provider, externalContactId, ownerUserId, snapshot, syncedAt);
        upsertProfile(tenantId, provider, externalContactId, userId, ownerUserId, snapshot, syncedAt);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 upsertOwner 流程中的校验、计算或对象转换。
     * @param externalContactId 业务对象 ID，用于定位具体记录。
     * @param ownerUserId 业务对象 ID，用于定位具体记录。
     * @param snapshot snapshot 参数，用于 upsertOwner 流程中的校验、计算或对象转换。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void upsertOwner(Long tenantId,
                             String provider,
                             String externalContactId,
                             String ownerUserId,
                             PrivateDomainContactSnapshot snapshot,
                             LocalDateTime syncedAt) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationPrivateContactOwnerDO row = ownerMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateContactOwnerDO>()
                        .eq(ConversationPrivateContactOwnerDO::getTenantId, tenantId)
                        .eq(ConversationPrivateContactOwnerDO::getProvider, provider)
                        .eq(ConversationPrivateContactOwnerDO::getExternalContactId, externalContactId)
                        .eq(ConversationPrivateContactOwnerDO::getOwnerUserId, ownerUserId)
                        .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new ConversationPrivateContactOwnerDO();
            row.setTenantId(tenantId);
            row.setProvider(provider);
            row.setExternalContactId(externalContactId);
            row.setOwnerUserId(ownerUserId);
            row.setCreatedAt(syncedAt);
        }
        row.setRemark(snapshot.remark());
        row.setState(snapshot.state());
        row.setAddWay(snapshot.addWay());
        row.setTagsJson(json(snapshot.tags()));
        row.setAttributesJson(json(snapshot.attributes()));
        row.setRawPayloadJson(json(snapshot.rawPayload()));
        row.setSyncedAt(syncedAt);
        row.setUpdatedAt(syncedAt);
        if (row.getId() == null) {
            ownerMapper.insert(row);
        } else {
            ownerMapper.updateById(row);
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 upsertProfile 流程中的校验、计算或对象转换。
     * @param externalContactId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param ownerUserId 业务对象 ID，用于定位具体记录。
     * @param snapshot snapshot 参数，用于 upsertProfile 流程中的校验、计算或对象转换。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void upsertProfile(Long tenantId,
                               String provider,
                               String externalContactId,
                               String userId,
                               String ownerUserId,
                               PrivateDomainContactSnapshot snapshot,
                               LocalDateTime syncedAt) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationContactProfileDO row = profileMapper.selectOne(
                new LambdaQueryWrapper<ConversationContactProfileDO>()
                        .eq(ConversationContactProfileDO::getTenantId, tenantId)
                        .eq(ConversationContactProfileDO::getUserId, userId)
                        .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new ConversationContactProfileDO();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setCreatedAt(syncedAt);
        }
        row.setDisplayName(snapshot.displayName());
        row.setExternalContactId(externalContactId);
        row.setPrivateDomainSource(provider);
        row.setOwner(ownerUserId);
        row.setLifecycleStage("PRIVATE_DOMAIN");
        row.setTagsJson(json(snapshot.tags()));
        row.setAttributesJson(json(snapshot.attributes()));
        row.setUpdatedAt(syncedAt);
        if (row.getId() == null) {
            profileMapper.insert(row);
        } else {
            profileMapper.updateById(row);
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 upsertGroup 流程中的校验、计算或对象转换。
     * @param snapshot snapshot 参数，用于 upsertGroup 流程中的校验、计算或对象转换。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    private int upsertGroup(Long tenantId, String provider, PrivateDomainGroupSnapshot snapshot, LocalDateTime syncedAt) {
        String externalGroupId = required(snapshot.externalGroupId(), "externalGroupId");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationPrivateGroupDO row = groupMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateGroupDO>()
                        .eq(ConversationPrivateGroupDO::getTenantId, tenantId)
                        .eq(ConversationPrivateGroupDO::getProvider, provider)
                        .eq(ConversationPrivateGroupDO::getExternalGroupId, externalGroupId)
                        .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new ConversationPrivateGroupDO();
            row.setTenantId(tenantId);
            row.setProvider(provider);
            row.setExternalGroupId(externalGroupId);
            row.setCreatedAt(syncedAt);
        }
        row.setName(defaultString(snapshot.name(), externalGroupId));
        row.setOwnerUserId(snapshot.ownerUserId());
        row.setStatus(defaultString(snapshot.status(), "ACTIVE").toUpperCase(Locale.ROOT));
        row.setMemberCount(snapshot.memberCount() == null ? snapshot.members().size() : snapshot.memberCount());
        row.setCreatedAtRemote(snapshot.createdAtRemote());
        row.setRawPayloadJson(json(snapshot.rawPayload()));
        row.setSyncedAt(syncedAt);
        row.setUpdatedAt(syncedAt);
        if (row.getId() == null) {
            groupMapper.insert(row);
        } else {
            groupMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return 1;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 upsertGroupMember 流程中的校验、计算或对象转换。
     * @param externalGroupId 业务对象 ID，用于定位具体记录。
     * @param snapshot snapshot 参数，用于 upsertGroupMember 流程中的校验、计算或对象转换。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void upsertGroupMember(Long tenantId,
                                   String provider,
                                   String externalGroupId,
                                   PrivateDomainGroupMemberSnapshot snapshot,
                                   LocalDateTime syncedAt) {
        String groupId = required(externalGroupId, "externalGroupId");
        String memberUserId = required(snapshot.memberUserId(), "memberUserId");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationPrivateGroupMemberDO row = memberMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateGroupMemberDO>()
                        .eq(ConversationPrivateGroupMemberDO::getTenantId, tenantId)
                        .eq(ConversationPrivateGroupMemberDO::getProvider, provider)
                        .eq(ConversationPrivateGroupMemberDO::getExternalGroupId, groupId)
                        .eq(ConversationPrivateGroupMemberDO::getMemberUserId, memberUserId)
                        .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new ConversationPrivateGroupMemberDO();
            row.setTenantId(tenantId);
            row.setProvider(provider);
            row.setExternalGroupId(groupId);
            row.setMemberUserId(memberUserId);
            row.setCreatedAt(syncedAt);
        }
        row.setMemberType(defaultString(snapshot.memberType(), "EXTERNAL").toUpperCase(Locale.ROOT));
        row.setDisplayName(snapshot.displayName());
        row.setUnionIdHash(snapshot.unionIdHash());
        row.setJoinTime(snapshot.joinTime());
        row.setRawPayloadJson(json(snapshot.rawPayload()));
        row.setSyncedAt(syncedAt);
        row.setUpdatedAt(syncedAt);
        if (row.getId() == null) {
            memberMapper.insert(row);
        } else {
            memberMapper.updateById(row);
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param syncType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param sourceCursor source cursor 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param nextCursor next cursor 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param contactCount contact count 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param contactUpserted contact upserted 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param groupCount group count 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param groupUpserted group upserted 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param memberCount member count 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param memberUpserted member upserted 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param failedCount failed count 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param errorMessage error message 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param completedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 insertRun 流程生成的业务结果。
     */
    private PrivateDomainSyncRunView insertRun(Long tenantId,
                                               String provider,
                                               String syncType,
                                               String status,
                                               String actor,
                                               String sourceCursor,
                                               String nextCursor,
                                               int contactCount,
                                               int contactUpserted,
                                               int groupCount,
                                               int groupUpserted,
                                               int memberCount,
                                               int memberUpserted,
                                               int failedCount,
                                               String errorMessage,
                                               Map<String, Object> metadata,
                                               LocalDateTime startedAt,
                                               LocalDateTime completedAt) {
        // 准备本次处理所需的上下文和中间变量。
        ConversationPrivateSyncRunDO row = new ConversationPrivateSyncRunDO();
        row.setTenantId(tenantId);
        row.setProvider(provider);
        row.setSyncType(syncType);
        row.setStatus(status);
        row.setRequestedBy(defaultString(actor, "system"));
        row.setSourceCursor(sourceCursor);
        row.setNextCursor(nextCursor);
        row.setContactCount(contactCount);
        row.setContactUpserted(contactUpserted);
        row.setGroupCount(groupCount);
        row.setGroupUpserted(groupUpserted);
        row.setMemberCount(memberCount);
        row.setMemberUpserted(memberUpserted);
        row.setFailedCount(failedCount);
        row.setErrorMessage(errorMessage);
        row.setMetadataJson(json(metadata));
        row.setStartedAt(startedAt);
        row.setCompletedAt(completedAt);
        row.setCreatedAt(startedAt);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(completedAt);
        syncRunMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toRunView(row);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param contact contact 参数，用于 toContactView 流程中的校验、计算或对象转换。
     * @param owner owner 参数，用于 toContactView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private PrivateDomainContactView toContactView(ConversationPrivateContactDO contact,
                                                   ConversationPrivateContactOwnerDO owner) {
        return new PrivateDomainContactView(
                contact.getId(),
                contact.getTenantId(),
                contact.getProvider(),
                contact.getExternalContactId(),
                contact.getUserId(),
                contact.getDisplayName(),
                owner == null ? null : owner.getOwnerUserId(),
                owner == null ? null : owner.getRemark(),
                owner == null ? null : owner.getState(),
                owner == null ? null : owner.getAddWay(),
                list(contact.getTagsJson()),
                map(contact.getAttributesJson()),
                contact.getSyncedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 toGroupView 流程中的校验、计算或对象转换。
     * @param group group 参数，用于 toGroupView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private PrivateDomainGroupView toGroupView(Long tenantId, String provider, ConversationPrivateGroupDO group) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<PrivateDomainGroupMemberView> members = safeList(memberMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateGroupMemberDO>()
                        .eq(ConversationPrivateGroupMemberDO::getTenantId, tenantId)
                        .eq(ConversationPrivateGroupMemberDO::getProvider, provider)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .eq(ConversationPrivateGroupMemberDO::getExternalGroupId, group.getExternalGroupId()))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> provider.equals(row.getProvider()))
                .map(this::toMemberView)
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PrivateDomainGroupView(
                group.getId(),
                group.getTenantId(),
                group.getProvider(),
                group.getExternalGroupId(),
                group.getName(),
                group.getOwnerUserId(),
                group.getStatus(),
                group.getMemberCount(),
                group.getCreatedAtRemote(),
                members,
                group.getSyncedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param member member 参数，用于 toMemberView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private PrivateDomainGroupMemberView toMemberView(ConversationPrivateGroupMemberDO member) {
        return new PrivateDomainGroupMemberView(
                member.getId(),
                member.getTenantId(),
                member.getProvider(),
                member.getExternalGroupId(),
                member.getMemberUserId(),
                member.getMemberType(),
                member.getDisplayName(),
                member.getUnionIdHash(),
                member.getJoinTime(),
                member.getSyncedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private PrivateDomainSyncRunView toRunView(ConversationPrivateSyncRunDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PrivateDomainSyncRunView(
                row.getId(),
                row.getTenantId(),
                row.getProvider(),
                row.getSyncType(),
                row.getStatus(),
                row.getRequestedBy(),
                row.getSourceCursor(),
                row.getNextCursor(),
                defaultInt(row.getContactCount()),
                defaultInt(row.getContactUpserted()),
                defaultInt(row.getGroupCount()),
                defaultInt(row.getGroupUpserted()),
                defaultInt(row.getMemberCount()),
                defaultInt(row.getMemberUpserted()),
                defaultInt(row.getFailedCount()),
                row.getErrorMessage(),
                map(row.getMetadataJson()),
                row.getStartedAt(),
                row.getCompletedAt());
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("private-domain sync JSON serialization failed", ex);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<String> list(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param provider provider 参数，用于 normalizeProvider 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeProvider(String provider) {
        return required(provider, "provider").toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 userId 流程，围绕 user id 完成校验、计算或结果组装。
     *
     * @param provider provider 参数，用于 userId 流程中的校验、计算或对象转换。
     * @param externalContactId 业务对象 ID，用于定位具体记录。
     * @return 返回 user id 生成的文本或业务键。
     */
    private String userId(String provider, String externalContactId) {
        return provider + ":" + externalContactId;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeText 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default int 计算得到的数量、金额或指标值。
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows.stream().filter(Objects::nonNull).toList();
    }
}
