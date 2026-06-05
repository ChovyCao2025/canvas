package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamSchemaDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamSchemaMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeSchemaServiceTest {

    @Test
    void firstSchemaVersionIsCompatibleAndPersistedWithHash() {
        CdpWarehouseStreamSchemaMapper mapper = mock(CdpWarehouseStreamSchemaMapper.class);
        CdpWarehouseRealtimeSchemaService service = service(mapper);

        CdpWarehouseRealtimeSchemaService.SchemaVersionView view = service.register(9L,
                command("pipe", "SOURCE", "1", schema("event_id", "STRING", false), null), "alice");

        assertThat(view.compatibilityStatus()).isEqualTo("COMPATIBLE");
        assertThat(view.schemaHash()).hasSize(64);
        ArgumentCaptor<CdpWarehouseStreamSchemaDO> row = ArgumentCaptor.forClass(CdpWarehouseStreamSchemaDO.class);
        verify(mapper).upsert(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getSchemaRole()).isEqualTo("SOURCE");
        assertThat(row.getValue().getRegisteredBy()).isEqualTo("alice");
    }

    @Test
    void backwardCompatibilityDetectsRemovedFieldAndNewRequiredField() {
        CdpWarehouseStreamSchemaMapper mapper = mock(CdpWarehouseStreamSchemaMapper.class);
        when(mapper.latestActive(9L, "pipe", "SOURCE"))
                .thenReturn(row("pipe", "SOURCE", "1", """
                        {"fields":[
                          {"name":"event_id","type":"STRING","nullable":false},
                          {"name":"amount","type":"DECIMAL","nullable":true}
                        ]}
                        """, "COMPATIBLE"));
        CdpWarehouseRealtimeSchemaService service = service(mapper);

        CdpWarehouseRealtimeSchemaService.SchemaVersionView view = service.register(9L,
                command("pipe", "SOURCE", "2", """
                        {"fields":[
                          {"name":"event_id","type":"STRING","nullable":false},
                          {"name":"tenant_id","type":"BIGINT","nullable":false}
                        ]}
                        """, "BACKWARD"), "alice");

        assertThat(view.compatibilityStatus()).isEqualTo("BREAKING");
        assertThat(view.compatibilityReason()).contains("field removed: amount");
        assertThat(view.compatibilityReason()).contains("new non-null field: tenant_id");
    }

    @Test
    void checkpointEvaluationWarnsOnUnknownVersionAndFailsOnBreakingVersion() {
        CdpWarehouseStreamSchemaMapper mapper = mock(CdpWarehouseStreamSchemaMapper.class);
        when(mapper.findActiveVersion(9L, "pipe", "SOURCE", "1"))
                .thenReturn(row("pipe", "SOURCE", "1", schema("event_id", "STRING", false), "COMPATIBLE"));
        when(mapper.findActiveVersion(9L, "pipe", "SINK", "2"))
                .thenReturn(row("pipe", "SINK", "2", schema("event_id", "STRING", false), "BREAKING"));
        CdpWarehouseRealtimeSchemaService service = service(mapper);

        CdpWarehouseRealtimeSchemaService.SchemaCheckpointEvaluation breaking =
                service.evaluateCheckpoint(9L, "pipe", "1", "2");
        CdpWarehouseRealtimeSchemaService.SchemaCheckpointEvaluation unknown =
                service.evaluateCheckpoint(9L, "pipe", "missing", null);

        assertThat(breaking.status()).isEqualTo("FAIL");
        assertThat(breaking.reasons()).anyMatch(reason -> reason.contains("sink schema version 2 is BREAKING"));
        assertThat(unknown.status()).isEqualTo("WARN");
        assertThat(unknown.reasons()).contains("source schema version missing is not registered");
    }

    @Test
    void registerRejectsSchemaWithoutFieldsArray() {
        CdpWarehouseRealtimeSchemaService service = service(mock(CdpWarehouseStreamSchemaMapper.class));

        assertThatThrownBy(() -> service.register(9L,
                command("pipe", "SOURCE", "1", "{\"properties\":{}}", null), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fields array");
    }

    @Test
    void listReturnsTenantScopedRows() {
        CdpWarehouseStreamSchemaMapper mapper = mock(CdpWarehouseStreamSchemaMapper.class);
        when(mapper.listVersions(9L, "pipe", "SOURCE", 100))
                .thenReturn(List.of(row("pipe", "SOURCE", "1", schema("event_id", "STRING", false), "COMPATIBLE")));
        CdpWarehouseRealtimeSchemaService service = service(mapper);

        List<CdpWarehouseRealtimeSchemaService.SchemaVersionView> rows =
                service.list(9L, "pipe", "SOURCE", 500);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).schemaVersion()).isEqualTo("1");
        verify(mapper).listVersions(9L, "pipe", "SOURCE", 100);
    }

    private CdpWarehouseRealtimeSchemaService service(CdpWarehouseStreamSchemaMapper mapper) {
        return new CdpWarehouseRealtimeSchemaService(mapper, new ObjectMapper());
    }

    private CdpWarehouseRealtimeSchemaService.SchemaVersionCommand command(
            String pipelineKey, String role, String version, String schemaJson, String policy) {
        return new CdpWarehouseRealtimeSchemaService.SchemaVersionCommand(
                pipelineKey, role, version, schemaJson, policy, true);
    }

    private CdpWarehouseStreamSchemaDO row(String pipelineKey,
                                           String role,
                                           String version,
                                           String schemaJson,
                                           String compatibilityStatus) {
        CdpWarehouseStreamSchemaDO row = new CdpWarehouseStreamSchemaDO();
        row.setId(1L);
        row.setTenantId(9L);
        row.setPipelineKey(pipelineKey);
        row.setSchemaRole(role);
        row.setSchemaVersion(version);
        row.setSchemaHash("hash");
        row.setSchemaJson(schemaJson);
        row.setCompatibilityPolicy("BACKWARD");
        row.setCompatibilityStatus(compatibilityStatus);
        row.setCompatibilityReason("compatibility failed");
        row.setActive(1);
        row.setRegisteredBy("alice");
        return row;
    }

    private String schema(String fieldName, String type, boolean nullable) {
        return """
                {"fields":[{"name":"%s","type":"%s","nullable":%s}]}
                """.formatted(fieldName, type, nullable);
    }
}
