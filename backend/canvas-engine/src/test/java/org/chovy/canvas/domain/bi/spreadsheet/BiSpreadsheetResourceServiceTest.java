package org.chovy.canvas.domain.bi.spreadsheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetVersionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetVersionMapper;
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

class BiSpreadsheetResourceServiceTest {

    @Test
    void migrationCreatesSpreadsheetResourceAndVersionTables() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V275__bi_big_screen_spreadsheet_resources.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_spreadsheet");
        assertThat(migration).contains("spreadsheet_key VARCHAR(128) NOT NULL");
        assertThat(migration).contains("sheet_json JSON NOT NULL");
        assertThat(migration).contains("data_binding_json JSON NULL");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_spreadsheet_version");
        assertThat(migration).contains("UNIQUE KEY uk_bi_spreadsheet_version");
    }

    @Test
    void saveDraftPersistsSheetsFormulasAndDataBinding() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiSpreadsheetMapper spreadsheetMapper = mock(BiSpreadsheetMapper.class);
        BiSpreadsheetVersionMapper versionMapper = mock(BiSpreadsheetVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(spreadsheetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(
                null,
                spreadsheet("campaign-weekly", "DRAFT", 1));
        BiSpreadsheetResourceService service = new BiSpreadsheetResourceService(
                workspaceMapper,
                spreadsheetMapper,
                versionMapper,
                new ObjectMapper());

        BiSpreadsheetResource resource = service.saveDraft(7L, "alice", sampleSpreadsheet());

        ArgumentCaptor<BiSpreadsheetDO> captor = ArgumentCaptor.forClass(BiSpreadsheetDO.class);
        verify(spreadsheetMapper).upsert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getSpreadsheetKey()).isEqualTo("campaign-weekly");
        assertThat(captor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(captor.getValue().getSheetJson()).contains("=SUM(B2:B8)");
        assertThat(captor.getValue().getDataBindingJson()).contains("canvas_daily_stats");
        assertThat(captor.getValue().getStyleJson()).contains("currency");
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.version()).isEqualTo(1);
        assertThat(resource.source()).isEqualTo("PERSISTED");
        assertThat(resource.id()).isEqualTo(88L);
    }

    @Test
    void publishWritesVersionSnapshot() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiSpreadsheetMapper spreadsheetMapper = mock(BiSpreadsheetMapper.class);
        BiSpreadsheetVersionMapper versionMapper = mock(BiSpreadsheetVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(spreadsheetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(
                spreadsheet("campaign-weekly", "DRAFT", 1),
                spreadsheet("campaign-weekly", "PUBLISHED", 2));
        BiSpreadsheetResourceService service = new BiSpreadsheetResourceService(
                workspaceMapper,
                spreadsheetMapper,
                versionMapper,
                new ObjectMapper());

        BiSpreadsheetResource resource = service.publish(7L, "alice", "campaign-weekly");

        verify(spreadsheetMapper).publish(7L, 5L, "campaign-weekly");
        ArgumentCaptor<BiSpreadsheetVersionDO> captor = ArgumentCaptor.forClass(BiSpreadsheetVersionDO.class);
        verify(versionMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getSpreadsheetKey()).isEqualTo("campaign-weekly");
        assertThat(captor.getValue().getVersion()).isEqualTo(2);
        assertThat(captor.getValue().getResourceJson()).contains("\"spreadsheetKey\":\"campaign-weekly\"");
        assertThat(captor.getValue().getPublishedBy()).isEqualTo("alice");
        assertThat(resource.status()).isEqualTo("PUBLISHED");
        assertThat(resource.version()).isEqualTo(2);
        assertThat(resource.id()).isEqualTo(88L);
    }

    @Test
    void restoreVersionSavesSnapshotBackAsDraft() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiSpreadsheetMapper spreadsheetMapper = mock(BiSpreadsheetMapper.class);
        BiSpreadsheetVersionMapper versionMapper = mock(BiSpreadsheetVersionMapper.class);
        BiSpreadsheetVersionDO snapshot = new BiSpreadsheetVersionDO();
        snapshot.setSpreadsheetId(88L);
        snapshot.setSpreadsheetKey("campaign-weekly");
        snapshot.setVersion(2);
        snapshot.setResourceJson("""
                {"spreadsheetKey":"campaign-weekly","name":"Campaign Weekly","description":"restore",
                 "sheets":[{"sheetKey":"summary","cells":{"B9":"=SUM(B2:B8)"}}],
                 "dataBinding":{"datasetKey":"canvas_daily_stats"},"style":{"B9":"currency"},
                 "status":"PUBLISHED","version":2,"source":"PERSISTED"}
                """);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(spreadsheetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(
                spreadsheet("campaign-weekly", "PUBLISHED", 2),
                spreadsheet("campaign-weekly", "PUBLISHED", 2),
                spreadsheet("campaign-weekly", "DRAFT", 2));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(snapshot);
        BiSpreadsheetResourceService service = new BiSpreadsheetResourceService(
                workspaceMapper,
                spreadsheetMapper,
                versionMapper,
                new ObjectMapper());

        BiSpreadsheetResource restored = service.restoreVersion(7L, "alice", "campaign-weekly", 2);

        ArgumentCaptor<BiSpreadsheetDO> captor = ArgumentCaptor.forClass(BiSpreadsheetDO.class);
        verify(spreadsheetMapper).upsert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(captor.getValue().getSheetJson()).contains("=SUM(B2:B8)");
        assertThat(restored.status()).isEqualTo("DRAFT");
    }

    private BiSpreadsheetResource sampleSpreadsheet() {
        return new BiSpreadsheetResource(
                null,
                "campaign-weekly",
                "Campaign Weekly",
                "Formatted weekly campaign report",
                List.of(Map.of("sheetKey", "summary", "cells", Map.of("B9", "=SUM(B2:B8)"))),
                Map.of("datasetKey", "canvas_daily_stats", "columns", List.of("stat_date", "total_executions")),
                Map.of("B9", "currency"),
                null,
                null,
                null);
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        return workspace;
    }

    private BiSpreadsheetDO spreadsheet(String key, String status, int version) {
        BiSpreadsheetDO row = new BiSpreadsheetDO();
        row.setId(88L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setSpreadsheetKey(key);
        row.setName("Campaign Weekly");
        row.setDescription("Formatted weekly campaign report");
        row.setSheetJson("[{\"sheetKey\":\"summary\",\"cells\":{\"B9\":\"=SUM(B2:B8)\"}}]");
        row.setDataBindingJson("{\"datasetKey\":\"canvas_daily_stats\"}");
        row.setStyleJson("{\"B9\":\"currency\"}");
        row.setStatus(status);
        row.setVersion(version);
        row.setUpdatedAt(LocalDateTime.of(2026, 6, 5, 12, 0));
        return row;
    }
}
