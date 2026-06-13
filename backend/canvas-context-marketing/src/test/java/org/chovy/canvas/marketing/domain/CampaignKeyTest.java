package org.chovy.canvas.marketing.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignKeyTest {

    @Test
    void normalizesKeysToStableLowercaseSlugs() {
        CampaignKey key = CampaignKey.of(" Spring Launch 2026! ", "campaignKey");

        assertThat(key.value()).isEqualTo("spring-launch-2026");
        assertThat(key.toString()).isEqualTo("spring-launch-2026");
    }

    @Test
    void rejectsBlankOrSymbolOnlyKeys() {
        assertThatThrownBy(() -> CampaignKey.of("   ", "campaignKey"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campaignKey is required");

        assertThatThrownBy(() -> CampaignKey.of(" !!! ", "resourceKey"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resourceKey is required");
    }
}
