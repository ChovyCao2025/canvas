package org.chovy.canvas.marketing.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证CampaignKey的关键兼容行为。
 */
class CampaignKeyTest {

    /**
     * 验证 normalizes keys to stable lowercase slugs 场景的兼容行为。
     */
    @Test
    void normalizesKeysToStableLowercaseSlugs() {
        CampaignKey key = CampaignKey.of(" Spring Launch 2026! ", "campaignKey");

        assertThat(key.value()).isEqualTo("spring-launch-2026");
        assertThat(key.toString()).isEqualTo("spring-launch-2026");
    }

    /**
     * 验证 rejects blank or symbol only keys 场景的兼容行为。
     */
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
