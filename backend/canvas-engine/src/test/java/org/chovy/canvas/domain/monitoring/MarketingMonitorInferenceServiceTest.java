package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorInferenceDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorInferenceMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorItemMapper;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.chovy.canvas.engine.llm.AiUsageAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorInferenceServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void analyzeRejectsCrossTenantItemWithoutPersistingInference() {
        Fixture fixture = fixture(null);
        when(fixture.itemMapper.selectById(100L)).thenReturn(item(99L, 100L));

        assertThatThrownBy(() -> fixture.service.analyze(7L, new MarketingMonitorInferenceCommand(
                100L,
                null,
                null,
                "gpt-monitor",
                "2026-06",
                false,
                Map.of(),
                1000,
                Map.of()), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monitor item is not found");

        verify(fixture.inferenceMapper, never()).insert(any(MarketingMonitorInferenceDO.class));
    }

    @Test
    void analyzePersistsDeterministicFallbackWithGovernanceHashesWhenForced() {
        FailingGenerator generator = new FailingGenerator();
        Fixture fixture = fixture(generator);
        when(fixture.itemMapper.selectById(100L)).thenReturn(item(7L, 100L));
        doAnswer(invocation -> {
            invocation.<MarketingMonitorInferenceDO>getArgument(0).setId(501L);
            return 1;
        }).when(fixture.inferenceMapper).insert(any(MarketingMonitorInferenceDO.class));
        ArgumentCaptor<MarketingMonitorInferenceDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorInferenceDO.class);

        MarketingMonitorInferenceView view = fixture.service.analyze(7L, new MarketingMonitorInferenceCommand(
                100L,
                88L,
                99L,
                "gpt-monitor",
                "2026-06",
                true,
                Map.of("temperature", 0.1),
                1000,
                Map.of("reason", "operator-check")), "operator-1");

        assertThat(generator.called).isFalse();
        assertThat(view.id()).isEqualTo(501L);
        assertThat(view.itemId()).isEqualTo(100L);
        assertThat(view.fallbackUsed()).isTrue();
        assertThat(view.providerStatus()).isEqualTo("LOCAL_FALLBACK");
        assertThat(view.sentimentLabel()).isEqualTo("NEGATIVE");
        assertThat(view.sentimentScore()).isLessThan(BigDecimal.ZERO);
        assertThat(view.riskFlags()).contains("SENSITIVE_REFUND", "SENSITIVE_PRIVACY");
        assertThat(view.evidence()).containsKey("keywordHits");
        verify(fixture.inferenceMapper).insert(captor.capture());
        MarketingMonitorInferenceDO row = captor.getValue();
        assertThat(row.getTenantId()).isEqualTo(7L);
        assertThat(row.getItemId()).isEqualTo(100L);
        assertThat(row.getSourceId()).isEqualTo(10L);
        assertThat(row.getProviderId()).isEqualTo(88L);
        assertThat(row.getTemplateId()).isEqualTo(99L);
        assertThat(row.getModelKey()).isEqualTo("gpt-monitor");
        assertThat(row.getModelVersion()).isEqualTo("2026-06");
        assertThat(row.getInputHash()).hasSize(64);
        assertThat(row.getPromptHash()).hasSize(64);
        assertThat(row.getInputHash()).isNotEqualTo(row.getPromptHash());
        assertThat(row.getRequestedBy()).isEqualTo("operator-1");
        assertThat(row.getRiskFlagsJson()).contains("SENSITIVE_REFUND", "SENSITIVE_PRIVACY");
    }

    @Test
    void analyzePersistsNormalizedGeneratorOutputWithBoundedScoreAndJsonFields() {
        MarketingMonitorInferenceGenerator generator = context -> new MarketingMonitorInferenceGenerationResult(
                88L,
                99L,
                "gpt-monitor",
                "2026-06",
                "SUCCESS",
                false,
                "mixed",
                new BigDecimal("-1.70000"),
                new BigDecimal("1.70000"),
                List.of(Map.of("name", "CompetitorX", "type", "COMPETITOR", "sentiment", "negative")),
                List.of("pricing", "support"),
                List.of("low confidence", "legal"),
                Map.of("summary", "CompetitorX support complaints dominate."),
                237L);
        Fixture fixture = fixture(generator);
        when(fixture.itemMapper.selectById(100L)).thenReturn(item(7L, 100L));
        doAnswer(invocation -> {
            invocation.<MarketingMonitorInferenceDO>getArgument(0).setId(502L);
            return 1;
        }).when(fixture.inferenceMapper).insert(any(MarketingMonitorInferenceDO.class));
        ArgumentCaptor<MarketingMonitorInferenceDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorInferenceDO.class);

        MarketingMonitorInferenceView view = fixture.service.analyze(7L, new MarketingMonitorInferenceCommand(
                100L,
                88L,
                99L,
                "gpt-monitor",
                "2026-06",
                false,
                Map.of(),
                1000,
                Map.of()), "operator-1");

        assertThat(view.fallbackUsed()).isFalse();
        assertThat(view.providerStatus()).isEqualTo("SUCCESS");
        assertThat(view.sentimentLabel()).isEqualTo("MIXED");
        assertThat(view.sentimentScore()).isEqualByComparingTo(new BigDecimal("-1.00000"));
        assertThat(view.confidence()).isEqualByComparingTo(new BigDecimal("1.00000"));
        assertThat(view.entities()).singleElement()
                .satisfies(entity -> assertThat(entity)
                        .containsEntry("name", "CompetitorX")
                        .containsEntry("type", "COMPETITOR"));
        assertThat(view.topics()).containsExactly("pricing", "support");
        assertThat(view.riskFlags()).containsExactly("LOW_CONFIDENCE", "LEGAL");
        assertThat(view.evidence()).containsEntry("summary", "CompetitorX support complaints dominate.");
        verify(fixture.inferenceMapper).insert(captor.capture());
        MarketingMonitorInferenceDO row = captor.getValue();
        assertThat(row.getEntitiesJson()).contains("CompetitorX");
        assertThat(row.getTopicsJson()).contains("pricing", "support");
        assertThat(row.getRiskFlagsJson()).contains("LOW_CONFIDENCE", "LEGAL");
        assertThat(row.getEvidenceJson()).contains("summary");
        assertThat(row.getLatencyMs()).isEqualTo(237L);
    }

    @Test
    void listFiltersByTenantSentimentModelStatusFallbackAndBoundsLimit() {
        Fixture fixture = fixture(null);
        List<MarketingMonitorInferenceDO> rows = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            rows.add(inference(7L, (long) i, "NEGATIVE", "gpt-monitor", "SUCCESS", false));
        }
        rows.add(inference(99L, 999L, "NEGATIVE", "gpt-monitor", "SUCCESS", false));
        rows.add(inference(7L, 1000L, "POSITIVE", "gpt-monitor", "SUCCESS", false));
        rows.add(inference(7L, 1001L, "NEGATIVE", "other-model", "SUCCESS", false));
        rows.add(inference(7L, 1002L, "NEGATIVE", "gpt-monitor", "PROVIDER_ERROR", false));
        rows.add(inference(7L, 1003L, "NEGATIVE", "gpt-monitor", "SUCCESS", true));
        when(fixture.inferenceMapper.selectList(any())).thenReturn(rows);

        List<MarketingMonitorInferenceView> views = fixture.service.list(7L,
                new MarketingMonitorInferenceQuery(
                        null,
                        "negative",
                        "gpt-monitor",
                        "success",
                        false,
                        500));

        assertThat(views).hasSize(100);
        assertThat(views).allSatisfy(view -> {
            assertThat(view.tenantId()).isEqualTo(7L);
            assertThat(view.sentimentLabel()).isEqualTo("NEGATIVE");
            assertThat(view.modelKey()).isEqualTo("gpt-monitor");
            assertThat(view.providerStatus()).isEqualTo("SUCCESS");
            assertThat(view.fallbackUsed()).isFalse();
        });
    }

    @Test
    void defaultLlmGeneratorUsesBuiltInMonitoringInferenceTemplate() {
        ObjectMapper objectMapper = new ObjectMapper();
        AiUsageAuditService auditService = new AiUsageAuditService();
        AiLlmGateway gateway = new AiLlmGateway(
                new AiProviderModelRegistryService(),
                new AiPromptTemplateService(objectMapper),
                auditService,
                objectMapper,
                List.of());
        LlmMarketingMonitorInferenceGenerator generator =
                new LlmMarketingMonitorInferenceGenerator(gateway, objectMapper);
        MarketingMonitorItemDO item = item(7L, 100L);
        MarketingMonitorInferenceCommand command = new MarketingMonitorInferenceCommand(
                100L,
                null,
                null,
                null,
                null,
                false,
                Map.of(),
                1000,
                Map.of());

        MarketingMonitorInferenceGenerationResult result = generator.generate(
                new MarketingMonitorInferenceGenerationContext(
                        7L,
                        item,
                        command,
                        Map.of("text", item.getTextContent(), "brandKey", item.getBrandKey()),
                        "a".repeat(64),
                        "b".repeat(64)));

        assertThat(result.templateId()).isEqualTo(LlmMarketingMonitorInferenceGenerator.DEFAULT_TEMPLATE_ID);
        assertThat(result.providerStatus()).isEqualTo(AiLlmGateway.STATUS_SUCCESS);
        assertThat(result.sentimentLabel()).isEqualTo("NEUTRAL");
        assertThat(result.riskFlags()).contains("GENERATOR_FALLBACK");
        assertThat(auditService.recent()).singleElement()
                .satisfies(event -> assertThat(event.templateId())
                        .isEqualTo(LlmMarketingMonitorInferenceGenerator.DEFAULT_TEMPLATE_ID));
    }

    private Fixture fixture(MarketingMonitorInferenceGenerator generator) {
        MarketingMonitorItemMapper itemMapper = mock(MarketingMonitorItemMapper.class);
        MarketingMonitorInferenceMapper inferenceMapper = mock(MarketingMonitorInferenceMapper.class);
        return new Fixture(
                itemMapper,
                inferenceMapper,
                new MarketingMonitorInferenceService(
                        itemMapper,
                        inferenceMapper,
                        generator,
                        new ObjectMapper(),
                        CLOCK));
    }

    private MarketingMonitorItemDO item(Long tenantId, Long id) {
        MarketingMonitorItemDO row = new MarketingMonitorItemDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setSourceId(10L);
        row.setExternalItemId("post-" + id);
        row.setSourceType("MANUAL");
        row.setSourceUrl("https://example.com/post-" + id);
        row.setAuthorKey("author-1");
        row.setBrandKey("our-brand");
        row.setTextContent("Refund request: CompetitorX has bad broken support and privacy concerns.");
        row.setLanguage("en");
        row.setPublishedAt(now());
        row.setIngestedAt(now());
        row.setRawPayloadJson("{\"provider\":\"manual\"}");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private MarketingMonitorInferenceDO inference(Long tenantId,
                                                  Long id,
                                                  String sentimentLabel,
                                                  String modelKey,
                                                  String status,
                                                  boolean fallbackUsed) {
        MarketingMonitorInferenceDO row = new MarketingMonitorInferenceDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setItemId(id + 100L);
        row.setSourceId(10L);
        row.setProviderId(88L);
        row.setTemplateId(99L);
        row.setModelKey(modelKey);
        row.setModelVersion("2026-06");
        row.setProviderStatus(status);
        row.setFallbackUsed(fallbackUsed);
        row.setInputHash("a".repeat(64));
        row.setPromptHash("b".repeat(64));
        row.setSentimentLabel(sentimentLabel);
        row.setSentimentScore(new BigDecimal("-0.50000"));
        row.setConfidence(new BigDecimal("0.80000"));
        row.setEntitiesJson("[{\"name\":\"CompetitorX\"}]");
        row.setTopicsJson("[\"pricing\"]");
        row.setRiskFlagsJson("[\"LEGAL\"]");
        row.setEvidenceJson("{\"summary\":\"sample\"}");
        row.setLatencyMs(12L);
        row.setRequestedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }

    private static class FailingGenerator implements MarketingMonitorInferenceGenerator {
        private boolean called;

        @Override
        public MarketingMonitorInferenceGenerationResult generate(MarketingMonitorInferenceGenerationContext context) {
            called = true;
            throw new AssertionError("generator should not be called when fallback is forced");
        }
    }

    private record Fixture(MarketingMonitorItemMapper itemMapper,
                           MarketingMonitorInferenceMapper inferenceMapper,
                           MarketingMonitorInferenceService service) {
    }
}
