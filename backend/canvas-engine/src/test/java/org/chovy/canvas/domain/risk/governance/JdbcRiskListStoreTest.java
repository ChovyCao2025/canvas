package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.dal.dataobject.RiskListDO;
import org.chovy.canvas.dal.dataobject.RiskListEntryDO;
import org.chovy.canvas.dal.mapper.RiskListEntryMapper;
import org.chovy.canvas.dal.mapper.RiskListMapper;
import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.domain.risk.runtime.RiskListType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcRiskListStoreTest {

    private final RiskListMapper listMapper = mock(RiskListMapper.class);
    private final RiskListEntryMapper entryMapper = mock(RiskListEntryMapper.class);
    private final JdbcRiskListStore store = new JdbcRiskListStore(
            listMapper,
            entryMapper,
            Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void persistsListMetadataAndHashedEntries() {
        when(entryMapper.insert(any(RiskListEntryDO.class))).thenAnswer(invocation -> {
            RiskListEntryDO row = invocation.getArgument(0);
            row.setId(99L);
            return 1;
        });
        RiskListView list = new RiskListView(7L, "blocked-device", RiskListType.BLACK,
                RiskSubjectType.DEVICE_ID, "ACTIVE", true, "alice");

        store.saveList(list);
        RiskListEntryView saved = store.saveEntry(new RiskListEntryView(0, 7L, "blocked-device",
                "sha256:8f04f5b4d7d6e3a1", "d***1", "fraud", "ops",
                null, null, "alice"));

        ArgumentCaptor<RiskListDO> listCaptor = ArgumentCaptor.forClass(RiskListDO.class);
        ArgumentCaptor<RiskListEntryDO> entryCaptor = ArgumentCaptor.forClass(RiskListEntryDO.class);
        verify(listMapper).insert(listCaptor.capture());
        verify(entryMapper).insert(entryCaptor.capture());

        assertThat(listCaptor.getValue().getListType()).isEqualTo("BLACK");
        assertThat(listCaptor.getValue().getSubjectType()).isEqualTo("DEVICE_ID");
        assertThat(entryCaptor.getValue().getSubjectHash()).isEqualTo("sha256:8f04f5b4d7d6e3a1");
        assertThat(entryCaptor.getValue().getSubjectMasked()).isEqualTo("d***1");
        assertThat(entryCaptor.getValue().getEffectiveFrom()).isEqualTo(LocalDateTime.parse("2026-06-09T00:00"));
        assertThat(entryCaptor.getValue().toString()).doesNotContain("device-1");
        assertThat(saved.id()).isEqualTo(99L);
    }

    @Test
    void loadsPersistedListAndEntries() {
        RiskListDO list = new RiskListDO();
        list.setTenantId(7L);
        list.setListKey("blocked-device");
        list.setListType("BLACK");
        list.setSubjectType("DEVICE_ID");
        list.setStatus("ACTIVE");
        list.setRequiresApproval(true);
        list.setOwner("alice");
        RiskListEntryDO entry = new RiskListEntryDO();
        entry.setId(99L);
        entry.setTenantId(7L);
        entry.setListKey("blocked-device");
        entry.setSubjectHash("sha256:device-1");
        entry.setSubjectMasked("d***1");
        entry.setReason("fraud");
        entry.setSource("ops");
        entry.setEffectiveFrom(LocalDateTime.parse("2026-06-09T00:00"));
        entry.setCreatedBy("alice");
        when(listMapper.selectOne(any())).thenReturn(list);
        when(entryMapper.selectList(any())).thenReturn(List.of(entry));

        assertThat(store.findList(7L, "blocked-device").orElseThrow().listType()).isEqualTo(RiskListType.BLACK);
        assertThat(store.entries(7L, "blocked-device")).containsExactly(new RiskListEntryView(
                99L,
                7L,
                "blocked-device",
                "sha256:device-1",
                "d***1",
                "fraud",
                "ops",
                Instant.parse("2026-06-09T00:00:00Z"),
                null,
                "alice"));
    }
}
