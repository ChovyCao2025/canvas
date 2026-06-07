package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationPrivateDomainSyncServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 11, 0);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void ingestSnapshotCreatesContactOwnerProfileAndSuccessRun() {
        Harness harness = harness();
        when(harness.contactMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(harness.ownerMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(harness.profileMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(harness.contactMapper.insert(any(ConversationPrivateContactDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationPrivateContactDO>getArgument(0).setId(100L);
            return 1;
        });
        when(harness.ownerMapper.insert(any(ConversationPrivateContactOwnerDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationPrivateContactOwnerDO>getArgument(0).setId(101L);
            return 1;
        });
        when(harness.profileMapper.insert(any(ConversationContactProfileDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationContactProfileDO>getArgument(0).setId(300L);
            return 1;
        });
        when(harness.syncRunMapper.insert(any(ConversationPrivateSyncRunDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationPrivateSyncRunDO>getArgument(0).setId(900L);
            return 1;
        });

        PrivateDomainSyncRunView run = harness.service.ingestSnapshot(7L, commandWithContact(), "operator-1");

        assertThat(run.id()).isEqualTo(900L);
        assertThat(run.provider()).isEqualTo("WECOM");
        assertThat(run.status()).isEqualTo("SUCCESS");
        assertThat(run.contactCount()).isEqualTo(1);
        assertThat(run.contactUpserted()).isEqualTo(1);

        ArgumentCaptor<ConversationPrivateContactDO> contactCaptor =
                ArgumentCaptor.forClass(ConversationPrivateContactDO.class);
        verify(harness.contactMapper).insert(contactCaptor.capture());
        assertThat(contactCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(contactCaptor.getValue().getProvider()).isEqualTo("WECOM");
        assertThat(contactCaptor.getValue().getExternalContactId()).isEqualTo("wm-001");
        assertThat(contactCaptor.getValue().getUserId()).isEqualTo("WECOM:wm-001");
        assertThat(contactCaptor.getValue().getTagsJson()).contains("vip");

        ArgumentCaptor<ConversationPrivateContactOwnerDO> ownerCaptor =
                ArgumentCaptor.forClass(ConversationPrivateContactOwnerDO.class);
        verify(harness.ownerMapper).insert(ownerCaptor.capture());
        assertThat(ownerCaptor.getValue().getOwnerUserId()).isEqualTo("sales-1");
        assertThat(ownerCaptor.getValue().getState()).isEqualTo("followup:campaign-a");

        ArgumentCaptor<ConversationContactProfileDO> profileCaptor =
                ArgumentCaptor.forClass(ConversationContactProfileDO.class);
        verify(harness.profileMapper).insert(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getUserId()).isEqualTo("WECOM:wm-001");
        assertThat(profileCaptor.getValue().getExternalContactId()).isEqualTo("wm-001");
        assertThat(profileCaptor.getValue().getPrivateDomainSource()).isEqualTo("WECOM");
        assertThat(profileCaptor.getValue().getOwner()).isEqualTo("sales-1");
    }

    @Test
    void ingestSnapshotUpdatesExistingContactOwnerGroupAndMembersIdempotently() {
        Harness harness = harness();
        when(harness.contactMapper.selectOne(any(Wrapper.class))).thenReturn(contact(100L, 7L));
        when(harness.ownerMapper.selectOne(any(Wrapper.class))).thenReturn(owner(101L, 7L));
        when(harness.profileMapper.selectOne(any(Wrapper.class))).thenReturn(profile(300L, 7L));
        when(harness.groupMapper.selectOne(any(Wrapper.class))).thenReturn(group(200L, 7L));
        when(harness.memberMapper.selectOne(any(Wrapper.class))).thenReturn(member(201L, 7L));
        when(harness.syncRunMapper.insert(any(ConversationPrivateSyncRunDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationPrivateSyncRunDO>getArgument(0).setId(901L);
            return 1;
        });

        PrivateDomainSyncRunView run = harness.service.ingestSnapshot(7L, commandWithContactAndGroup(), "operator-1");

        assertThat(run.groupCount()).isEqualTo(1);
        assertThat(run.memberCount()).isEqualTo(1);
        verify(harness.contactMapper).updateById(any(ConversationPrivateContactDO.class));
        verify(harness.ownerMapper).updateById(any(ConversationPrivateContactOwnerDO.class));
        verify(harness.profileMapper).updateById(any(ConversationContactProfileDO.class));
        verify(harness.groupMapper).updateById(any(ConversationPrivateGroupDO.class));
        verify(harness.memberMapper).updateById(any(ConversationPrivateGroupMemberDO.class));
        verify(harness.contactMapper, never()).insert(any(ConversationPrivateContactDO.class));
        verify(harness.groupMapper, never()).insert(any(ConversationPrivateGroupDO.class));
    }

    @Test
    void ingestSnapshotRejectsBlankExternalIdsAndRecordsFailedRun() {
        Harness harness = harness();
        when(harness.syncRunMapper.insert(any(ConversationPrivateSyncRunDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationPrivateSyncRunDO>getArgument(0).setId(902L);
            return 1;
        });
        PrivateDomainSyncCommand command = new PrivateDomainSyncCommand(
                "wecom",
                "CONTACTS",
                "cursor-1",
                null,
                List.of(new PrivateDomainContactSnapshot(
                        " ",
                        "Broken",
                        null,
                        null,
                        null,
                        null,
                        "sales-1",
                        null,
                        null,
                        null,
                        List.of(),
                        Map.of(),
                        Map.of())),
                List.of(),
                Map.of("collector", "test"));

        assertThatThrownBy(() -> harness.service.ingestSnapshot(7L, command, "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalContactId");

        ArgumentCaptor<ConversationPrivateSyncRunDO> runCaptor =
                ArgumentCaptor.forClass(ConversationPrivateSyncRunDO.class);
        verify(harness.syncRunMapper).insert(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(runCaptor.getValue().getFailedCount()).isEqualTo(1);
        assertThat(runCaptor.getValue().getErrorMessage()).contains("externalContactId");
    }

    @Test
    void queriesContactsGroupsAndRunsWithinTenantAndBoundsLimit() {
        Harness harness = harness();
        ConversationPrivateContactDO contact = contact(100L, 7L);
        ConversationPrivateContactDO otherTenantContact = contact(101L, 8L);
        when(harness.contactMapper.selectList(any(Wrapper.class))).thenReturn(List.of(contact, otherTenantContact));
        when(harness.ownerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(owner(102L, 7L)));
        when(harness.groupMapper.selectList(any(Wrapper.class))).thenReturn(List.of(group(200L, 7L), group(201L, 8L)));
        when(harness.memberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(member(300L, 7L), member(301L, 8L)));
        when(harness.syncRunMapper.selectList(any(Wrapper.class))).thenReturn(List.of(syncRun(900L, 7L), syncRun(901L, 8L)));

        List<PrivateDomainContactView> contacts = harness.service.contacts(
                7L,
                new PrivateDomainContactQuery("wecom", "sales-1", "alice", 500));
        List<PrivateDomainGroupView> groups = harness.service.groups(
                7L,
                new PrivateDomainGroupQuery("wecom", "sales-1", 500));
        List<PrivateDomainSyncRunView> runs = harness.service.syncRuns(7L, "wecom", 500);

        assertThat(contacts).singleElement()
                .satisfies(view -> assertThat(view.externalContactId()).isEqualTo("wm-001"));
        assertThat(groups).singleElement()
                .satisfies(view -> {
                    assertThat(view.externalGroupId()).isEqualTo("wr-001");
                    assertThat(view.members()).singleElement()
                            .satisfies(member -> assertThat(member.memberUserId()).isEqualTo("wm-001"));
                });
        assertThat(runs).singleElement()
                .satisfies(view -> assertThat(view.id()).isEqualTo(900L));
    }

    private Harness harness() {
        ConversationPrivateContactMapper contactMapper = mock(ConversationPrivateContactMapper.class);
        ConversationPrivateContactOwnerMapper ownerMapper = mock(ConversationPrivateContactOwnerMapper.class);
        ConversationPrivateGroupMapper groupMapper = mock(ConversationPrivateGroupMapper.class);
        ConversationPrivateGroupMemberMapper memberMapper = mock(ConversationPrivateGroupMemberMapper.class);
        ConversationPrivateSyncRunMapper syncRunMapper = mock(ConversationPrivateSyncRunMapper.class);
        ConversationContactProfileMapper profileMapper = mock(ConversationContactProfileMapper.class);
        ConversationPrivateDomainSyncService service = new ConversationPrivateDomainSyncService(
                contactMapper,
                ownerMapper,
                groupMapper,
                memberMapper,
                syncRunMapper,
                profileMapper,
                OBJECT_MAPPER,
                CLOCK);
        return new Harness(service, contactMapper, ownerMapper, groupMapper, memberMapper, syncRunMapper, profileMapper);
    }

    private PrivateDomainSyncCommand commandWithContact() {
        return new PrivateDomainSyncCommand(
                "wecom",
                "CONTACTS",
                "cursor-1",
                "cursor-2",
                List.of(contactSnapshot()),
                List.of(),
                Map.of("collector", "wecom-cron"));
    }

    private PrivateDomainSyncCommand commandWithContactAndGroup() {
        return new PrivateDomainSyncCommand(
                "wecom",
                "FULL",
                "cursor-1",
                "cursor-2",
                List.of(contactSnapshot()),
                List.of(new PrivateDomainGroupSnapshot(
                        "wr-001",
                        "VIP Leads",
                        "sales-1",
                        "ACTIVE",
                        1,
                        NOW.minusHours(2),
                        List.of(new PrivateDomainGroupMemberSnapshot(
                                "wm-001",
                                "EXTERNAL",
                                "Alice Zhang",
                                "sha256:abc",
                                NOW.minusHours(1),
                                Map.of("userid", "wm-001"))),
                        Map.of("chat_id", "wr-001"))),
                Map.of("collector", "wecom-cron"));
    }

    private PrivateDomainContactSnapshot contactSnapshot() {
        return new PrivateDomainContactSnapshot(
                "wm-001",
                "Alice Zhang",
                "https://cdn.example.com/a.png",
                "Example Ltd",
                "FEMALE",
                "sha256:abc",
                "sales-1",
                "VIP lead",
                "followup:campaign-a",
                "GROUP_QR",
                List.of("vip", "demo"),
                Map.of("city", "Shanghai"),
                Map.of("external_userid", "wm-001"));
    }

    private ConversationPrivateContactDO contact(Long id, Long tenantId) {
        ConversationPrivateContactDO row = new ConversationPrivateContactDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setProvider("WECOM");
        row.setExternalContactId("wm-001");
        row.setUserId("WECOM:wm-001");
        row.setDisplayName("Alice Zhang");
        row.setTagsJson("[\"vip\"]");
        row.setAttributesJson("{\"city\":\"Shanghai\"}");
        row.setSyncedAt(NOW);
        return row;
    }

    private ConversationPrivateContactOwnerDO owner(Long id, Long tenantId) {
        ConversationPrivateContactOwnerDO row = new ConversationPrivateContactOwnerDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setProvider("WECOM");
        row.setExternalContactId("wm-001");
        row.setOwnerUserId("sales-1");
        row.setRemark("VIP lead");
        row.setState("followup:campaign-a");
        row.setSyncedAt(NOW);
        return row;
    }

    private ConversationContactProfileDO profile(Long id, Long tenantId) {
        ConversationContactProfileDO row = new ConversationContactProfileDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setUserId("WECOM:wm-001");
        return row;
    }

    private ConversationPrivateGroupDO group(Long id, Long tenantId) {
        ConversationPrivateGroupDO row = new ConversationPrivateGroupDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setProvider("WECOM");
        row.setExternalGroupId("wr-001");
        row.setName("VIP Leads");
        row.setOwnerUserId("sales-1");
        row.setStatus("ACTIVE");
        row.setMemberCount(1);
        row.setSyncedAt(NOW);
        return row;
    }

    private ConversationPrivateGroupMemberDO member(Long id, Long tenantId) {
        ConversationPrivateGroupMemberDO row = new ConversationPrivateGroupMemberDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setProvider("WECOM");
        row.setExternalGroupId("wr-001");
        row.setMemberUserId("wm-001");
        row.setMemberType("EXTERNAL");
        row.setDisplayName("Alice Zhang");
        row.setSyncedAt(NOW);
        return row;
    }

    private ConversationPrivateSyncRunDO syncRun(Long id, Long tenantId) {
        ConversationPrivateSyncRunDO row = new ConversationPrivateSyncRunDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setProvider("WECOM");
        row.setSyncType("FULL");
        row.setStatus("SUCCESS");
        row.setRequestedBy("operator-1");
        row.setContactCount(1);
        row.setContactUpserted(1);
        row.setGroupCount(1);
        row.setGroupUpserted(1);
        row.setMemberCount(1);
        row.setMemberUpserted(1);
        row.setFailedCount(0);
        row.setStartedAt(NOW);
        row.setCompletedAt(NOW);
        return row;
    }

    private record Harness(
            ConversationPrivateDomainSyncService service,
            ConversationPrivateContactMapper contactMapper,
            ConversationPrivateContactOwnerMapper ownerMapper,
            ConversationPrivateGroupMapper groupMapper,
            ConversationPrivateGroupMemberMapper memberMapper,
            ConversationPrivateSyncRunMapper syncRunMapper,
            ConversationContactProfileMapper profileMapper) {
    }
}
