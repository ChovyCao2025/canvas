package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketingMonitorWebhookPayloadMapperTest {

    @Test
    void mapsGenericMentionPayloadWithSourceMetadataFallbacks() {
        MarketingMonitorWebhookPayloadMapper mapper =
                new MarketingMonitorWebhookPayloadMapper(new ObjectMapper());
        MarketingMonitorSourceDO source = source("{\"defaultBrandKey\":\"our-brand\","
                + "\"competitors\":{\"competitorx\":[\"CompetitorX\",\"CX\"]}}");

        MarketingMonitorItemIngestCommand command = mapper.toIngestCommand(source, Map.of(
                "id", "mention-1",
                "message", "CompetitorX has bad support",
                "url", "https://social.example/posts/mention-1",
                "author", Map.of("id", "author-1"),
                "lang", "en",
                "created_at", "2026-06-06T11:30:00+08:00"));

        assertThat(command.sourceId()).isEqualTo(10L);
        assertThat(command.externalItemId()).isEqualTo("mention-1");
        assertThat(command.sourceUrl()).isEqualTo("https://social.example/posts/mention-1");
        assertThat(command.authorKey()).isEqualTo("author-1");
        assertThat(command.brandKey()).isEqualTo("our-brand");
        assertThat(command.text()).isEqualTo("CompetitorX has bad support");
        assertThat(command.language()).isEqualTo("en");
        assertThat(command.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 11, 30));
        assertThat(command.competitors()).containsEntry("competitorx", List.of("CompetitorX", "CX"));
        assertThat(command.rawPayload()).containsEntry("id", "mention-1");
    }

    @Test
    void mapsPayloadCompetitorListAndRejectsMissingText() {
        MarketingMonitorWebhookPayloadMapper mapper =
                new MarketingMonitorWebhookPayloadMapper(new ObjectMapper());
        MarketingMonitorSourceDO source = source("{}");

        MarketingMonitorItemIngestCommand command = mapper.toIngestCommand(source, Map.of(
                "external_item_id", "mention-2",
                "text", "CX is slow",
                "competitors", List.of(Map.of("key", "competitorx", "terms", List.of("CX")))));

        assertThat(command.competitors()).containsEntry("competitorx", List.of("CX"));

        assertThatThrownBy(() -> mapper.toIngestCommand(source, Map.of("id", "missing-text")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text");
    }

    private MarketingMonitorSourceDO source(String metadataJson) {
        MarketingMonitorSourceDO source = new MarketingMonitorSourceDO();
        source.setId(10L);
        source.setTenantId(7L);
        source.setSourceKey("brandwatch");
        source.setSourceType("GENERIC_SOCIAL");
        source.setEnabled(1);
        source.setWebhookEnabled(1);
        source.setMetadataJson(metadataJson);
        return source;
    }
}
