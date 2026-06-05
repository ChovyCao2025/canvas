package org.chovy.canvas.domain.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;
import org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper;
import org.chovy.canvas.engine.audience.StableUserIndexService;
import org.chovy.canvas.engine.audience.VersionedAudienceBitmapStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.roaringbitmap.RoaringBitmap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceMaterializationServiceTest {

    @Test
    void successfulMaterializationWritesReadyBitmapAndRun() {
        AudienceMaterializationService.AudienceDefinitionRepository definitions =
                mock(AudienceMaterializationService.AudienceDefinitionRepository.class);
        AudienceMaterializationService.BehaviorAudienceOlapRepository olap =
                mock(AudienceMaterializationService.BehaviorAudienceOlapRepository.class);
        StableUserIndexService indexService = mock(StableUserIndexService.class);
        VersionedAudienceBitmapStore bitmapStore = mock(VersionedAudienceBitmapStore.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceDefinitionDO definition = definition(ruleJson());
        when(definitions.requireEnabled(7L, 10L)).thenReturn(definition);
        when(olap.findMatchingUsers(any())).thenReturn(List.of("u1", "u2"));
        when(indexService.getOrCreateIndex(7L, "u1")).thenReturn(11L);
        when(indexService.getOrCreateIndex(7L, "u2")).thenReturn(12L);
        when(bitmapStore.saveVersion(eq(7L), eq(10L), eq(1L), any(RoaringBitmap.class), eq("OLAP")))
                .thenReturn("audience:bitmap:10:v:1");
        AudienceMaterializationService service = service(definitions, olap, indexService, bitmapStore, runMapper);

        AudienceMaterializationService.MaterializationResult result =
                service.materialize(7L, 10L, "operator");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.matchedUsers()).isEqualTo(2L);
        verify(bitmapStore).markReady(7L, 10L, 1L);
        ArgumentCaptor<RoaringBitmap> bitmap = ArgumentCaptor.forClass(RoaringBitmap.class);
        verify(bitmapStore).saveVersion(eq(7L), eq(10L), eq(1L), bitmap.capture(), eq("OLAP"));
        assertThat(bitmap.getValue().contains(11)).isTrue();
        assertThat(bitmap.getValue().contains(12)).isTrue();
        verify(runMapper).insert(any(AudienceMaterializationRunDO.class));
        verify(runMapper).updateById(org.mockito.ArgumentMatchers.<AudienceMaterializationRunDO>argThat(run ->
                "SUCCESS".equals(run.getStatus())));
    }

    @Test
    void failedOlapQueryRecordsFailedRunWithoutPublishingBitmap() {
        AudienceMaterializationService.AudienceDefinitionRepository definitions =
                mock(AudienceMaterializationService.AudienceDefinitionRepository.class);
        AudienceMaterializationService.BehaviorAudienceOlapRepository olap =
                mock(AudienceMaterializationService.BehaviorAudienceOlapRepository.class);
        VersionedAudienceBitmapStore bitmapStore = mock(VersionedAudienceBitmapStore.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        when(definitions.requireEnabled(7L, 10L)).thenReturn(definition(ruleJson()));
        when(olap.findMatchingUsers(any())).thenThrow(new IllegalStateException("doris unavailable"));
        AudienceMaterializationService service = service(definitions, olap,
                mock(StableUserIndexService.class), bitmapStore, runMapper);

        AudienceMaterializationService.MaterializationResult result =
                service.materialize(7L, 10L, "operator");

        assertThat(result.status()).isEqualTo("FAILED");
        verify(bitmapStore, never()).markReady(any(), any(), any());
        verify(runMapper).updateById(org.mockito.ArgumentMatchers.<AudienceMaterializationRunDO>argThat(run ->
                "FAILED".equals(run.getStatus()) && run.getErrorMessage().contains("doris unavailable")));
    }

    private AudienceMaterializationService service(
            AudienceMaterializationService.AudienceDefinitionRepository definitions,
            AudienceMaterializationService.BehaviorAudienceOlapRepository olap,
            StableUserIndexService indexService,
            VersionedAudienceBitmapStore bitmapStore,
            AudienceMaterializationRunMapper runMapper) {
        return new AudienceMaterializationService(
                definitions,
                olap,
                new BehaviorAudienceRuleCompiler(new ObjectMapper()),
                indexService,
                bitmapStore,
                runMapper,
                1000);
    }

    private AudienceDefinitionDO definition(String ruleJson) {
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setId(10L);
        definition.setTenantId(7L);
        definition.setName("Paid twice");
        definition.setRuleJson(ruleJson);
        definition.setEnabled(1);
        return definition;
    }

    private String ruleJson() {
        return """
                {"source":"CDP_EVENT_METRIC","eventCode":"OrderPaid","windowDays":30,"metric":"COUNT","operator":">=","value":2}
                """;
    }
}
