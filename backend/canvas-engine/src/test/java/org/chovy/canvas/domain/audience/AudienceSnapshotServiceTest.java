package org.chovy.canvas.domain.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.AudienceSnapshotMode;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceSnapshotDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceSnapshotMapper;
import org.chovy.canvas.engine.audience.AudienceSnapshotService;
import org.chovy.canvas.engine.audience.AudienceUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceSnapshotServiceTest {

    @Test
    void lockSnapshotPersistsDistinctUsersAndMetadata() {
        AudienceUserResolver resolver = mock(AudienceUserResolver.class);
        AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
        when(resolver.resolve(101L)).thenReturn(List.of("u1", "u2", "u1", ""));
        AudienceSnapshotService service = new AudienceSnapshotService(
                resolver,
                snapshotMapper,
                mock(AudienceDefinitionMapper.class),
                new ObjectMapper(),
                10_000);

        AudienceSnapshotDO snapshot = service.lockSnapshot(101L, 62L, 91L, "audience_node", "alice");

        ArgumentCaptor<AudienceSnapshotDO> captor = ArgumentCaptor.forClass(AudienceSnapshotDO.class);
        verify(snapshotMapper).insert(captor.capture());
        assertThat(captor.getValue().getAudienceId()).isEqualTo(101L);
        assertThat(captor.getValue().getCanvasId()).isEqualTo(62L);
        assertThat(captor.getValue().getCanvasVersionId()).isEqualTo(91L);
        assertThat(captor.getValue().getNodeId()).isEqualTo("audience_node");
        assertThat(captor.getValue().getUserCount()).isEqualTo(2L);
        assertThat(captor.getValue().getUserIdsJson()).contains("u1", "u2");
        assertThat(snapshot.getSnapshotMode()).isEqualTo(AudienceSnapshotMode.STATIC_LOCKED.name());
    }

    @Test
    void lockSnapshotRejectsOversizedAudience() {
        AudienceUserResolver resolver = mock(AudienceUserResolver.class);
        when(resolver.resolve(101L)).thenReturn(List.of("u1", "u2"));
        AudienceSnapshotService service = new AudienceSnapshotService(
                resolver,
                mock(AudienceSnapshotMapper.class),
                mock(AudienceDefinitionMapper.class),
                new ObjectMapper(),
                1);

        assertThatThrownBy(() -> service.lockSnapshot(101L, 62L, 91L, "audience_node", "alice"))
                .hasMessageContaining("AUDIENCE_SNAPSHOT_LIMIT");
    }

    @Test
    void defaultModeFallsBackToStaticLockedWhenDefinitionIsBlank() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setDefaultSnapshotMode("");
        when(definitionMapper.selectById(101L)).thenReturn(definition);
        AudienceSnapshotService service = new AudienceSnapshotService(
                mock(AudienceUserResolver.class),
                mock(AudienceSnapshotMapper.class),
                definitionMapper,
                new ObjectMapper(),
                10_000);

        assertThat(service.defaultModeForAudience(101L)).isEqualTo(AudienceSnapshotMode.STATIC_LOCKED);
    }

    @Test
    void bindGraphLocksStaticNodesAndRemovesDynamicStaleSnapshotIds() {
        AudienceUserResolver resolver = mock(AudienceUserResolver.class);
        AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
        when(resolver.resolve(101L)).thenReturn(List.of("u1"));
        doAnswer(invocation -> {
            AudienceSnapshotDO snapshot = invocation.getArgument(0);
            snapshot.setId(501L);
            return 1;
        }).when(snapshotMapper).insert(any(AudienceSnapshotDO.class));
        AudienceSnapshotService service = new AudienceSnapshotService(
                resolver,
                snapshotMapper,
                mock(AudienceDefinitionMapper.class),
                new ObjectMapper(),
                10_000);
        String graphJson = """
                {"nodes":[
                  {"id":"static","type":"TAGGER","config":{"mode":"audience","audienceId":101,"audienceSnapshotMode":"STATIC_LOCKED"}},
                  {"id":"dynamic","type":"TAGGER","config":{"mode":"audience","audienceId":102,"audienceSnapshotMode":"DYNAMIC_REFRESH","audienceSnapshotId":9}}
                ]}
                """;

        String bound = service.bindAudienceSnapshotsForPublish(62L, 91L, graphJson, "alice");

        assertThat(bound).contains("\"audienceSnapshotMode\":\"STATIC_LOCKED\"");
        assertThat(bound).contains("\"audienceSnapshotId\":501");
        assertThat(bound).contains("\"audienceSnapshotMode\":\"DYNAMIC_REFRESH\"");
        assertThat(bound).doesNotContain("\"audienceSnapshotId\":9");
    }

    @Test
    void bindGraphUsesAudienceDefaultWhenNodeModeIsBlank() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setDefaultSnapshotMode("DYNAMIC_REFRESH");
        when(definitionMapper.selectById(101L)).thenReturn(definition);
        AudienceSnapshotService service = new AudienceSnapshotService(
                mock(AudienceUserResolver.class),
                mock(AudienceSnapshotMapper.class),
                definitionMapper,
                new ObjectMapper(),
                10_000);
        String graphJson = """
                {"nodes":[
                  {"id":"dynamic","type":"TAGGER","config":{"mode":"audience","audienceId":101,"audienceSnapshotId":9}}
                ]}
                """;

        String bound = service.bindAudienceSnapshotsForPublish(62L, 91L, graphJson, "alice");

        assertThat(bound).contains("\"audienceSnapshotMode\":\"DYNAMIC_REFRESH\"");
        assertThat(bound).doesNotContain("\"audienceSnapshotId\":9");
    }

    @Test
    void usersReadsSnapshotJsonArray() {
        AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
        AudienceSnapshotDO snapshot = new AudienceSnapshotDO();
        snapshot.setId(501L);
        snapshot.setUserIdsJson("[\"u1\",\"u2\"]");
        when(snapshotMapper.selectById(501L)).thenReturn(snapshot);
        AudienceSnapshotService service = new AudienceSnapshotService(
                mock(AudienceUserResolver.class),
                snapshotMapper,
                mock(AudienceDefinitionMapper.class),
                new ObjectMapper(),
                10_000);

        assertThat(service.users(501L)).containsExactly("u1", "u2");
        assertThat(service.contains(501L, "u2")).isTrue();
    }

    @Test
    void usersRejectsMissingSnapshot() {
        AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
        when(snapshotMapper.selectById(501L)).thenReturn(null);
        AudienceSnapshotService service = new AudienceSnapshotService(
                mock(AudienceUserResolver.class),
                snapshotMapper,
                mock(AudienceDefinitionMapper.class),
                new ObjectMapper(),
                10_000);

        assertThatThrownBy(() -> service.users(501L))
                .hasMessageContaining("Audience snapshot not found");
    }
}
