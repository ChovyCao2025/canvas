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
        } catch (RuntimeException ex) {
            insertRun(scopedTenantId, provider, syncType, "FAILED", actor,
                    command.sourceCursor(), command.nextCursor(), contactCount, 0,
                    groupCount, 0, memberCount, 0, 1, ex.getMessage(),
                    command.metadata(), startedAt, now());
            throw ex;
        }
    }

    public List<PrivateDomainContactView> contacts(Long tenantId, PrivateDomainContactQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("private-domain contact query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeProvider(query.provider());
        int limit = boundedLimit(query.limit());
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

    public List<PrivateDomainGroupView> groups(Long tenantId, PrivateDomainGroupQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("private-domain group query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeProvider(query.provider());
        int limit = boundedLimit(query.limit());
        return safeList(groupMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateGroupDO>()
                        .eq(ConversationPrivateGroupDO::getTenantId, scopedTenantId)
                        .eq(ConversationPrivateGroupDO::getProvider, provider)
                        .eq(hasText(query.ownerUserId()), ConversationPrivateGroupDO::getOwnerUserId, query.ownerUserId())
                        .orderByDesc(ConversationPrivateGroupDO::getSyncedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> provider.equals(row.getProvider()))
                .limit(limit)
                .map(row -> toGroupView(scopedTenantId, provider, row))
                .toList();
    }

    public List<PrivateDomainSyncRunView> syncRuns(Long tenantId, String provider, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedProvider = normalizeProvider(provider);
        int boundedLimit = boundedLimit(limit);
        return safeList(syncRunMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateSyncRunDO>()
                        .eq(ConversationPrivateSyncRunDO::getTenantId, scopedTenantId)
                        .eq(ConversationPrivateSyncRunDO::getProvider, scopedProvider)
                        .orderByDesc(ConversationPrivateSyncRunDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> scopedProvider.equals(row.getProvider()))
                .limit(boundedLimit)
                .map(this::toRunView)
                .toList();
    }

    private void upsertContact(Long tenantId,
                               String provider,
                               PrivateDomainContactSnapshot snapshot,
                               LocalDateTime syncedAt) {
        String externalContactId = required(snapshot.externalContactId(), "externalContactId");
        String ownerUserId = required(snapshot.ownerUserId(), "ownerUserId");
        String userId = userId(provider, externalContactId);
        ConversationPrivateContactDO row = contactMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateContactDO>()
                        .eq(ConversationPrivateContactDO::getTenantId, tenantId)
                        .eq(ConversationPrivateContactDO::getProvider, provider)
                        .eq(ConversationPrivateContactDO::getExternalContactId, externalContactId)
                        .last("LIMIT 1"));
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

    private void upsertOwner(Long tenantId,
                             String provider,
                             String externalContactId,
                             String ownerUserId,
                             PrivateDomainContactSnapshot snapshot,
                             LocalDateTime syncedAt) {
        ConversationPrivateContactOwnerDO row = ownerMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateContactOwnerDO>()
                        .eq(ConversationPrivateContactOwnerDO::getTenantId, tenantId)
                        .eq(ConversationPrivateContactOwnerDO::getProvider, provider)
                        .eq(ConversationPrivateContactOwnerDO::getExternalContactId, externalContactId)
                        .eq(ConversationPrivateContactOwnerDO::getOwnerUserId, ownerUserId)
                        .last("LIMIT 1"));
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

    private void upsertProfile(Long tenantId,
                               String provider,
                               String externalContactId,
                               String userId,
                               String ownerUserId,
                               PrivateDomainContactSnapshot snapshot,
                               LocalDateTime syncedAt) {
        ConversationContactProfileDO row = profileMapper.selectOne(
                new LambdaQueryWrapper<ConversationContactProfileDO>()
                        .eq(ConversationContactProfileDO::getTenantId, tenantId)
                        .eq(ConversationContactProfileDO::getUserId, userId)
                        .last("LIMIT 1"));
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

    private int upsertGroup(Long tenantId, String provider, PrivateDomainGroupSnapshot snapshot, LocalDateTime syncedAt) {
        String externalGroupId = required(snapshot.externalGroupId(), "externalGroupId");
        ConversationPrivateGroupDO row = groupMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateGroupDO>()
                        .eq(ConversationPrivateGroupDO::getTenantId, tenantId)
                        .eq(ConversationPrivateGroupDO::getProvider, provider)
                        .eq(ConversationPrivateGroupDO::getExternalGroupId, externalGroupId)
                        .last("LIMIT 1"));
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
        return 1;
    }

    private void upsertGroupMember(Long tenantId,
                                   String provider,
                                   String externalGroupId,
                                   PrivateDomainGroupMemberSnapshot snapshot,
                                   LocalDateTime syncedAt) {
        String groupId = required(externalGroupId, "externalGroupId");
        String memberUserId = required(snapshot.memberUserId(), "memberUserId");
        ConversationPrivateGroupMemberDO row = memberMapper.selectOne(
                new LambdaQueryWrapper<ConversationPrivateGroupMemberDO>()
                        .eq(ConversationPrivateGroupMemberDO::getTenantId, tenantId)
                        .eq(ConversationPrivateGroupMemberDO::getProvider, provider)
                        .eq(ConversationPrivateGroupMemberDO::getExternalGroupId, groupId)
                        .eq(ConversationPrivateGroupMemberDO::getMemberUserId, memberUserId)
                        .last("LIMIT 1"));
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
        row.setUpdatedAt(completedAt);
        syncRunMapper.insert(row);
        return toRunView(row);
    }

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

    private PrivateDomainGroupView toGroupView(Long tenantId, String provider, ConversationPrivateGroupDO group) {
        List<PrivateDomainGroupMemberView> members = safeList(memberMapper.selectList(
                new LambdaQueryWrapper<ConversationPrivateGroupMemberDO>()
                        .eq(ConversationPrivateGroupMemberDO::getTenantId, tenantId)
                        .eq(ConversationPrivateGroupMemberDO::getProvider, provider)
                        .eq(ConversationPrivateGroupMemberDO::getExternalGroupId, group.getExternalGroupId()))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> provider.equals(row.getProvider()))
                .map(this::toMemberView)
                .toList();
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

    private PrivateDomainSyncRunView toRunView(ConversationPrivateSyncRunDO row) {
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

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("private-domain sync JSON serialization failed", ex);
        }
    }

    private List<String> list(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeProvider(String provider) {
        return required(provider, "provider").toUpperCase(Locale.ROOT);
    }

    private String userId(String provider, String externalContactId) {
        return provider + ":" + externalContactId;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String normalizeText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows.stream().filter(Objects::nonNull).toList();
    }
}
