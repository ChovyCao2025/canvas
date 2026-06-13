package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignLink;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingCampaignPersistenceConverterTest {

    private final MarketingCampaignPersistenceConverter converter = new MarketingCampaignPersistenceConverter();

    @Test
    void campaignDoPreservesTableAndColumnMappingShape() {
        assertThat(MarketingCampaignMasterDO.class.getAnnotation(TableName.class).value())
                .isEqualTo("marketing_campaign_master");
        assertThat(BaseMapper.class).isAssignableFrom(MarketingCampaignMasterMapper.class);

        MarketingCampaignMasterDO row = converter.toCampaignRow(MarketingCampaign.createExisting(
                10L,
                7L,
                CampaignKey.of("spring-launch", "campaignKey"),
                "Spring launch",
                "ACQUISITION",
                CampaignStatus.ACTIVE,
                "CRM",
                "Growth",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-30T23:59:00"),
                new BigDecimal("1200.50"),
                "USD",
                Map.of("northStar", "signup"),
                "operator-1",
                "operator-2",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-02T00:00:00")));

        assertThat(row.getId()).isEqualTo(10L);
        assertThat(row.getTenantId()).isEqualTo(7L);
        assertThat(row.getCampaignKey()).isEqualTo("spring-launch");
        assertThat(row.getBudgetAmount()).isEqualByComparingTo("1200.50");
        assertThat(row.getBriefJson()).contains("\"northStar\":\"signup\"");

        MarketingCampaign mapped = converter.toCampaign(row);
        assertThat(mapped.campaignKey().value()).isEqualTo("spring-launch");
        assertThat(mapped.brief()).containsEntry("northStar", "signup");
    }

    @Test
    void linkDoPreservesTableMapperAndLaunchFlagMapping() {
        assertThat(MarketingCampaignLinkDO.class.getAnnotation(TableName.class).value())
                .isEqualTo("marketing_campaign_link");
        assertThat(BaseMapper.class).isAssignableFrom(MarketingCampaignLinkMapper.class);

        MarketingCampaignLinkDO row = converter.toLinkRow(MarketingCampaignLink.createExisting(
                20L,
                7L,
                10L,
                "JOURNEY",
                300L,
                CampaignKey.of("launch-journey", "resourceKey"),
                "Launch journey",
                "/canvas/300",
                "PRIMARY",
                "ACTIVE",
                true,
                Map.of("stage", "launch"),
                "operator-1",
                "operator-2",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-02T00:00:00")));

        assertThat(row.getId()).isEqualTo(20L);
        assertThat(row.getTenantId()).isEqualTo(7L);
        assertThat(row.getCampaignId()).isEqualTo(10L);
        assertThat(row.getResourceType()).isEqualTo("JOURNEY");
        assertThat(row.getResourceKey()).isEqualTo("launch-journey");
        assertThat(row.getRequiredForLaunch()).isEqualTo(1);
        assertThat(row.getMetadataJson()).contains("\"stage\":\"launch\"");

        MarketingCampaignLink mapped = converter.toLink(row);
        assertThat(mapped.requiredForLaunch()).isTrue();
        assertThat(mapped.metadata()).containsEntry("stage", "launch");
    }

    @Test
    void jsonMappingPreservesNestedListsAndNullValues() {
        Map<String, Object> brief = new LinkedHashMap<>();
        brief.put("northStar", "signup");
        brief.put("optional", null);
        brief.put("steps", List.of("draft", "launch"));
        brief.put("limits", Map.of("daily", 10));

        MarketingCampaignMasterDO row = converter.toCampaignRow(MarketingCampaign.createExisting(
                10L,
                7L,
                CampaignKey.of("spring-launch", "campaignKey"),
                "Spring launch",
                "ACQUISITION",
                CampaignStatus.ACTIVE,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                "CNY",
                brief,
                "operator-1",
                "operator-1",
                null,
                null));

        assertThat(row.getBriefJson()).contains("\"optional\":null");
        assertThat(row.getBriefJson()).contains("\"steps\":[\"draft\",\"launch\"]");
        assertThat(row.getBriefJson()).contains("\"limits\":{\"daily\":10}");

        MarketingCampaign mapped = converter.toCampaign(row);
        assertThat(mapped.brief()).containsEntry("optional", null);
        assertThat(mapped.brief()).containsEntry("steps", List.of("draft", "launch"));
        assertThat(mapped.brief()).containsEntry("limits", Map.of("daily", 10));
    }
}
