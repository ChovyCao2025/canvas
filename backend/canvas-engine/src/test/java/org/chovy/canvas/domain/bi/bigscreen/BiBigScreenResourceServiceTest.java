package org.chovy.canvas.domain.bi.bigscreen;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiBigScreenVersionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiBigScreenVersionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiBigScreenResourceServiceTest {

    @Test
    void migrationCreatesBigScreenResourceAndVersionTables() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V275__bi_big_screen_spreadsheet_resources.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_big_screen");
        assertThat(migration).contains("screen_key VARCHAR(128) NOT NULL");
        assertThat(migration).contains("layout_json JSON NOT NULL");
        assertThat(migration).contains("mobile_layout_json JSON NULL");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_big_screen_version");
        assertThat(migration).contains("UNIQUE KEY uk_bi_big_screen_version");
    }

    @Test
    void saveDraftPersistsFreeformLayoutAndMobileVariant() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiBigScreenMapper screenMapper = mock(BiBigScreenMapper.class);
        BiBigScreenVersionMapper versionMapper = mock(BiBigScreenVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(screenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, screen("ops-war-room", "DRAFT", 1));
        BiBigScreenResourceService service = new BiBigScreenResourceService(
                workspaceMapper,
                screenMapper,
                versionMapper,
                new ObjectMapper());

        BiBigScreenResource resource = service.saveDraft(7L, "alice", sampleScreen());

        ArgumentCaptor<BiBigScreenDO> captor = ArgumentCaptor.forClass(BiBigScreenDO.class);
        verify(screenMapper).upsert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getScreenKey()).isEqualTo("ops-war-room");
        assertThat(captor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(captor.getValue().getLayoutJson()).contains("kpi-total");
        assertThat(captor.getValue().getMobileLayoutJson()).contains("phone");
        assertThat(captor.getValue().getRefreshJson()).contains("30");
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.version()).isEqualTo(1);
        assertThat(resource.source()).isEqualTo("PERSISTED");
        assertThat(resource.id()).isEqualTo(99L);
    }

    @Test
    void publishWritesVersionSnapshot() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiBigScreenMapper screenMapper = mock(BiBigScreenMapper.class);
        BiBigScreenVersionMapper versionMapper = mock(BiBigScreenVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(screenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(
                screen("ops-war-room", "DRAFT", 1),
                screen("ops-war-room", "PUBLISHED", 2));
        BiBigScreenResourceService service = new BiBigScreenResourceService(
                workspaceMapper,
                screenMapper,
                versionMapper,
                new ObjectMapper());

        BiBigScreenResource resource = service.publish(7L, "alice", "ops-war-room");

        verify(screenMapper).publish(7L, 5L, "ops-war-room");
        ArgumentCaptor<BiBigScreenVersionDO> captor = ArgumentCaptor.forClass(BiBigScreenVersionDO.class);
        verify(versionMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getScreenKey()).isEqualTo("ops-war-room");
        assertThat(captor.getValue().getVersion()).isEqualTo(2);
        assertThat(captor.getValue().getResourceJson()).contains("\"screenKey\":\"ops-war-room\"");
        assertThat(captor.getValue().getPublishedBy()).isEqualTo("alice");
        assertThat(resource.status()).isEqualTo("PUBLISHED");
        assertThat(resource.version()).isEqualTo(2);
        assertThat(resource.id()).isEqualTo(99L);
    }

    @Test
    void restoreVersionSavesSnapshotBackAsDraft() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiBigScreenMapper screenMapper = mock(BiBigScreenMapper.class);
        BiBigScreenVersionMapper versionMapper = mock(BiBigScreenVersionMapper.class);
        BiBigScreenVersionDO snapshot = new BiBigScreenVersionDO();
        snapshot.setScreenId(99L);
        snapshot.setScreenKey("ops-war-room");
        snapshot.setVersion(2);
        snapshot.setResourceJson("""
                {"screenKey":"ops-war-room","name":"Ops War Room","description":"restore",
                 "size":{"width":1920,"height":1080},"background":{"color":"#101820"},
                 "layout":[{"widgetKey":"kpi-total","x":0,"y":0,"w":6,"h":4}],
                 "refresh":{"intervalSeconds":60},"mobileLayout":{"phone":[{"widgetKey":"kpi-total","order":1}]},
                 "status":"PUBLISHED","version":2,"source":"PERSISTED"}
                """);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(screenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(
                screen("ops-war-room", "PUBLISHED", 2),
                screen("ops-war-room", "PUBLISHED", 2),
                screen("ops-war-room", "DRAFT", 2));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(snapshot);
        BiBigScreenResourceService service = new BiBigScreenResourceService(
                workspaceMapper,
                screenMapper,
                versionMapper,
                new ObjectMapper());

        BiBigScreenResource restored = service.restoreVersion(7L, "alice", "ops-war-room", 2);

        ArgumentCaptor<BiBigScreenDO> captor = ArgumentCaptor.forClass(BiBigScreenDO.class);
        verify(screenMapper).upsert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(captor.getValue().getLayoutJson()).contains("kpi-total");
        assertThat(restored.status()).isEqualTo("DRAFT");
    }

    private BiBigScreenResource sampleScreen() {
        return new BiBigScreenResource(
                null,
                "ops-war-room",
                "Ops War Room",
                "Campaign command center",
                Map.of("width", 1920, "height", 1080),
                Map.of("color", "#101820"),
                List.of(Map.of("widgetKey", "kpi-total", "x", 0, "y", 0, "w", 6, "h", 4)),
                Map.of("intervalSeconds", 30),
                Map.of("phone", List.of(Map.of("widgetKey", "kpi-total", "order", 1))),
                null,
                null,
                null);
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        return workspace;
    }

    private BiBigScreenDO screen(String key, String status, int version) {
        BiBigScreenDO row = new BiBigScreenDO();
        row.setId(99L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setScreenKey(key);
        row.setName("Ops War Room");
        row.setDescription("Campaign command center");
        row.setSizeJson("{\"width\":1920,\"height\":1080}");
        row.setBackgroundJson("{\"color\":\"#101820\"}");
        row.setLayoutJson("[{\"widgetKey\":\"kpi-total\",\"x\":0,\"y\":0,\"w\":6,\"h\":4}]");
        row.setRefreshJson("{\"intervalSeconds\":30}");
        row.setMobileLayoutJson("{\"phone\":[{\"widgetKey\":\"kpi-total\",\"order\":1}]}");
        row.setStatus(status);
        row.setVersion(version);
        row.setUpdatedAt(LocalDateTime.of(2026, 6, 5, 12, 0));
        return row;
    }
}
