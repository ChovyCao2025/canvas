package org.chovy.canvas.engine.insights;

import org.chovy.canvas.dal.dataobject.AudienceComputeRunDO;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceStatDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.AudienceComputeRunMapper;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MauticInspiredInsightServiceTest {

    @Test
    void explainsAudienceMembershipFromReadyStatAndBitmap() {
        AudienceDefinitionMapper audienceMapper = mock(AudienceDefinitionMapper.class);
        AudienceStatMapper statMapper = mock(AudienceStatMapper.class);
        AudienceComputeRunMapper runMapper = mock(AudienceComputeRunMapper.class);
        AudienceBitmapStore bitmapStore = mock(AudienceBitmapStore.class);
        AudienceDefinitionDO audience = audience(10L, "VIP");
        AudienceStatDO stat = stat("READY", 42L);
        AudienceComputeRunDO run = new AudienceComputeRunDO();
        run.setStatus("SUCCEEDED");
        run.setEstimatedSize(42L);
        when(audienceMapper.selectById(10L)).thenReturn(audience);
        when(statMapper.selectById(10L)).thenReturn(stat);
        when(runMapper.selectList(any())).thenReturn(List.of(run));
        when(bitmapStore.isMember(10L, "user-1")).thenReturn(true);

        MauticInspiredInsightService.AudienceMembershipReport report = service(
                audienceMapper, statMapper, runMapper, bitmapStore).explainAudienceMembership(10L, "user-1");

        assertThat(report.status()).isEqualTo("MATCHED");
        assertThat(report.audienceName()).isEqualTo("VIP");
        assertThat(report.latestRunStatus()).isEqualTo("SUCCEEDED");
        assertThat(report.evidence()).contains("bitmap membership matched");
    }

    @Test
    void resolvesChannelPreferenceAroundSuppressionsAndAvailability() {
        CustomerChannelMapper channelMapper = mock(CustomerChannelMapper.class);
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        CustomerChannelDO sms = channel("SMS", 1, "13800000000");
        CustomerChannelDO email = channel("EMAIL", 1, "u@example.com");
        MarketingSuppressionDO suppressedEmail = suppression("EMAIL", 1, null);
        when(channelMapper.selectList(any())).thenReturn(List.of(sms, email));
        when(suppressionMapper.selectList(any())).thenReturn(List.of(suppressedEmail));

        MauticInspiredInsightService.ChannelPreferenceReport report = service(channelMapper, suppressionMapper)
                .resolveChannelPreference("user-1", "EMAIL");

        assertThat(report.recommendedChannel()).isEqualTo("SMS");
        assertThat(report.fallbackChannel()).isNull();
        assertThat(report.channels()).extracting(MauticInspiredInsightService.ChannelCandidate::state)
                .containsExactly("ELIGIBLE", "SUPPRESSED");
    }

    @Test
    void buildsJourneyPathAndPublishHealthEvidence() {
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        when(traceMapper.selectList(any())).thenReturn(List.of(
                trace("start", "START", 1),
                trace("send", "SEND_MESSAGE", 3)));
        CanvasDO canvas = new CanvasDO();
        canvas.setId(9L);
        canvas.setName("Welcome");
        canvas.setStatus(1);
        canvas.setPublishedVersionId(90L);
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(90L);
        version.setGraphJson("{\"nodes\":[{\"type\":\"START\"},{\"type\":\"SEND_MESSAGE\"}],\"edges\":[{}]}");
        when(canvasMapper.selectById(9L)).thenReturn(canvas);
        when(versionMapper.selectById(90L)).thenReturn(version);
        MauticInspiredInsightService insight = service(traceMapper, canvasMapper, versionMapper);

        MauticInspiredInsightService.JourneyPathReport path = insight.explainJourneyPath("exec-1");
        MauticInspiredInsightService.PublishHealthReport health = insight.publishHealth(9L);

        assertThat(path.steps()).extracting(MauticInspiredInsightService.JourneyStep::reason)
                .containsExactly("节点执行成功", "节点未进入当前执行路径或被策略跳过");
        assertThat(health.score()).isEqualTo(100);
        assertThat(health.checks()).allMatch(MauticInspiredInsightService.HealthCheck::passed);
    }

    @Test
    void returnsSuppressionTimelineAndFrequencyTemplates() {
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        when(suppressionMapper.selectList(any())).thenReturn(List.of(
                suppression("SMS", 1, LocalDateTime.now().plusDays(1)),
                suppression("EMAIL", 0, LocalDateTime.now().minusDays(1))));

        MauticInspiredInsightService insight = service(mock(CustomerChannelMapper.class), suppressionMapper);

        assertThat(insight.suppressionTimeline("user-1").records())
                .extracting(MauticInspiredInsightService.SuppressionRecord::state)
                .containsExactly("ACTIVE", "INACTIVE");
        assertThat(insight.frequencyTemplates()).extracting(MauticInspiredInsightService.FrequencyTemplate::scope)
                .contains("GLOBAL", "JOURNEY", "CHANNEL", "NODE");
    }

    private MauticInspiredInsightService service(AudienceDefinitionMapper audienceMapper,
                                                 AudienceStatMapper statMapper,
                                                 AudienceComputeRunMapper runMapper,
                                                 AudienceBitmapStore bitmapStore) {
        return new MauticInspiredInsightService(audienceMapper, statMapper, runMapper, bitmapStore,
                mock(CanvasExecutionTraceMapper.class), mock(CustomerChannelMapper.class),
                mock(MarketingSuppressionMapper.class), mock(CanvasMapper.class), mock(CanvasVersionMapper.class));
    }

    private MauticInspiredInsightService service(CustomerChannelMapper channelMapper,
                                                 MarketingSuppressionMapper suppressionMapper) {
        return new MauticInspiredInsightService(mock(AudienceDefinitionMapper.class), mock(AudienceStatMapper.class),
                mock(AudienceComputeRunMapper.class), mock(AudienceBitmapStore.class), mock(CanvasExecutionTraceMapper.class),
                channelMapper, suppressionMapper, mock(CanvasMapper.class), mock(CanvasVersionMapper.class));
    }

    private MauticInspiredInsightService service(CanvasExecutionTraceMapper traceMapper,
                                                 CanvasMapper canvasMapper,
                                                 CanvasVersionMapper versionMapper) {
        return new MauticInspiredInsightService(mock(AudienceDefinitionMapper.class), mock(AudienceStatMapper.class),
                mock(AudienceComputeRunMapper.class), mock(AudienceBitmapStore.class), traceMapper,
                mock(CustomerChannelMapper.class), mock(MarketingSuppressionMapper.class), canvasMapper, versionMapper);
    }

    private AudienceDefinitionDO audience(Long id, String name) {
        AudienceDefinitionDO row = new AudienceDefinitionDO();
        row.setId(id);
        row.setName(name);
        row.setEnabled(1);
        row.setRuleJson("{\"conditions\":[{\"field\":\"level\",\"operator\":\"eq\",\"value\":\"VIP\"}]}");
        return row;
    }

    private AudienceStatDO stat(String status, Long size) {
        AudienceStatDO row = new AudienceStatDO();
        row.setStatus(status);
        row.setEstimatedSize(size);
        row.setComputedAt(LocalDateTime.of(2026, 6, 5, 10, 0));
        return row;
    }

    private CustomerChannelDO channel(String channel, int enabled, String address) {
        CustomerChannelDO row = new CustomerChannelDO();
        row.setUserId("user-1");
        row.setChannel(channel);
        row.setEnabled(enabled);
        row.setAddress(address);
        return row;
    }

    private MarketingSuppressionDO suppression(String channel, int active, LocalDateTime expiresAt) {
        MarketingSuppressionDO row = new MarketingSuppressionDO();
        row.setUserId("user-1");
        row.setChannel(channel);
        row.setReason("operator");
        row.setActive(active);
        row.setExpiresAt(expiresAt);
        row.setCreatedAt(LocalDateTime.of(2026, 6, 5, 9, 0));
        return row;
    }

    private CanvasExecutionTraceDO trace(String nodeId, String nodeType, int status) {
        return CanvasExecutionTraceDO.builder()
                .executionId("exec-1")
                .nodeId(nodeId)
                .nodeName(nodeId)
                .nodeType(nodeType)
                .status(status)
                .startedAt(LocalDateTime.of(2026, 6, 5, 9, status))
                .build();
    }
}
