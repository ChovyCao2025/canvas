package org.chovy.canvas.domain.creator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CreatorCampaignDO;
import org.chovy.canvas.dal.dataobject.CreatorCollaborationDO;
import org.chovy.canvas.dal.dataobject.CreatorDeliverableDO;
import org.chovy.canvas.dal.dataobject.CreatorProfileDO;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.mapper.CreatorCollaborationMapper;
import org.chovy.canvas.dal.mapper.CreatorDeliverableMapper;
import org.chovy.canvas.dal.mapper.CreatorProfileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

class CreatorCollaborationServiceTest {

    @Test
    void upsertsCreatorWithNormalizedProviderHandleAndJsonMetadata() {
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        doAnswer(invocation -> {
            invocation.<CreatorProfileDO>getArgument(0).setId(10L);
            return 1;
        }).when(profileMapper).insert(any(CreatorProfileDO.class));
        CreatorCollaborationService service = service(profileMapper);

        CreatorProfileView view = service.upsertCreator(7L, new CreatorProfileCommand(
                "tiktok",
                " @Creator.One ",
                "Creator One",
                "KOC",
                "video",
                120000L,
                new BigDecimal("0.052300"),
                List.of("beauty", "vip"),
                "active",
                "normal",
                Map.of("region", "CN")), "operator-1");

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.provider()).isEqualTo("TIKTOK");
        assertThat(view.handleKey()).isEqualTo("creator.one");
        assertThat(view.tags()).containsExactly("beauty", "vip");
        ArgumentCaptor<CreatorProfileDO> captor = ArgumentCaptor.forClass(CreatorProfileDO.class);
        verify(profileMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getProvider()).isEqualTo("TIKTOK");
        assertThat(captor.getValue().getHandle()).isEqualTo("@Creator.One");
        assertThat(captor.getValue().getHandleKey()).isEqualTo("creator.one");
        assertThat(captor.getValue().getTagsJson()).contains("beauty", "vip");
        assertThat(captor.getValue().getMetadataJson()).contains("\"region\":\"CN\"");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
    }

    @Test
    void upsertsCampaignByTenantAndCampaignKey() {
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCampaignDO existing = campaign();
        when(campaignMapper.selectOne(any())).thenReturn(existing);
        CreatorCollaborationService service = service(mock(CreatorProfileMapper.class), campaignMapper,
                mock(CreatorCollaborationMapper.class), mock(CreatorDeliverableMapper.class));

        CreatorCampaignView view = service.upsertCampaign(7L, new CreatorCampaignCommand(
                "summer-drop",
                "Summer Drop",
                "conversion",
                new BigDecimal("50000.0000"),
                "usd",
                now().minusDays(1),
                now().plusDays(30),
                "active",
                Map.of("brief", "launch")), "operator-1");

        assertThat(view.id()).isEqualTo(20L);
        assertThat(view.campaignKey()).isEqualTo("summer-drop");
        assertThat(view.currency()).isEqualTo("USD");
        verify(campaignMapper).updateById(org.mockito.ArgumentMatchers.<CreatorCampaignDO>argThat(row ->
                row.getId().equals(20L)
                        && row.getTenantId().equals(7L)
                        && row.getCampaignName().equals("Summer Drop")
                        && row.getStatus().equals("ACTIVE")
                        && row.getMetadataJson().contains("launch")));
    }

    @Test
    void rejectsCollaborationWhenCreatorBelongsToAnotherTenant() {
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorProfileDO creator = creator();
        creator.setTenantId(99L);
        when(profileMapper.selectById(10L)).thenReturn(creator);
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        CreatorCollaborationService service = service(profileMapper, campaignMapper, collaborationMapper,
                mock(CreatorDeliverableMapper.class));

        assertThatThrownBy(() -> service.upsertCollaboration(7L, collaborationCommand(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creator");

        verify(collaborationMapper, never()).insert(any(CreatorCollaborationDO.class));
        verify(collaborationMapper, never()).updateById(any(CreatorCollaborationDO.class));
    }

    @Test
    void summaryCalculatesPerformanceCostRoiAndOverdueDeliverablesWithTenantScope() {
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorDeliverableMapper deliverableMapper = mock(CreatorDeliverableMapper.class);
        when(collaborationMapper.selectList(any())).thenReturn(List.of(collaboration(), otherTenantCollaboration()));
        when(deliverableMapper.selectList(any())).thenReturn(List.of(
                deliverable("POSTED", now().minusDays(2), 1000L, 10L, 2L, 3L, 5L, 20L, 2L,
                        new BigDecimal("200.0000")),
                deliverable("PLANNED", now().minusDays(1), 500L, 1L, 1L, 0L, 0L, 5L, 0L,
                        BigDecimal.ZERO),
                otherTenantDeliverable()));
        CreatorCollaborationService service = service(mock(CreatorProfileMapper.class),
                mock(CreatorCampaignMapper.class), collaborationMapper, deliverableMapper);

        CreatorPerformanceSummaryView summary = service.summary(7L, new CreatorPerformanceSummaryQuery(
                20L,
                10L,
                null,
                now()));

        assertThat(summary.deliverableCount()).isEqualTo(2);
        assertThat(summary.postedDeliverables()).isEqualTo(1);
        assertThat(summary.overdueDeliverables()).isEqualTo(1);
        assertThat(summary.impressionCount()).isEqualTo(1500L);
        assertThat(summary.engagementCount()).isEqualTo(22L);
        assertThat(summary.clickCount()).isEqualTo(25L);
        assertThat(summary.conversionCount()).isEqualTo(2L);
        assertThat(summary.revenueAmount()).isEqualByComparingTo("200.0000");
        assertThat(summary.fixedFeeAmount()).isEqualByComparingTo("100.0000");
        assertThat(summary.commissionAmount()).isEqualByComparingTo("20.0000");
        assertThat(summary.totalCostAmount()).isEqualByComparingTo("120.0000");
        assertThat(summary.roi()).isEqualByComparingTo("0.666667");
    }

    private CreatorCollaborationService service(CreatorProfileMapper profileMapper) {
        return service(profileMapper, mock(CreatorCampaignMapper.class), mock(CreatorCollaborationMapper.class),
                mock(CreatorDeliverableMapper.class));
    }

    private CreatorCollaborationService service(CreatorProfileMapper profileMapper,
                                                CreatorCampaignMapper campaignMapper,
                                                CreatorCollaborationMapper collaborationMapper,
                                                CreatorDeliverableMapper deliverableMapper) {
        return new CreatorCollaborationService(
                profileMapper,
                campaignMapper,
                collaborationMapper,
                deliverableMapper,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC")));
    }

    private CreatorCollaborationCommand collaborationCommand() {
        return new CreatorCollaborationCommand(
                20L,
                10L,
                "affiliate",
                new BigDecimal("100.0000"),
                new BigDecimal("0.100000"),
                "https://trk.example/c1",
                "SUMMER10",
                "active",
                Map.of("usage", "partnership_ads"),
                Map.of("note", "first wave"));
    }

    private CreatorProfileDO creator() {
        CreatorProfileDO row = new CreatorProfileDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("TIKTOK");
        row.setHandle("@creator.one");
        row.setHandleKey("creator.one");
        row.setDisplayName("Creator One");
        row.setStatus("ACTIVE");
        row.setRiskStatus("NORMAL");
        return row;
    }

    private CreatorCampaignDO campaign() {
        CreatorCampaignDO row = new CreatorCampaignDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setCampaignKey("summer-drop");
        row.setCampaignName("Summer Drop");
        row.setBudgetAmount(new BigDecimal("50000.0000"));
        row.setCurrency("USD");
        row.setStatus("ACTIVE");
        return row;
    }

    private CreatorCollaborationDO collaboration() {
        CreatorCollaborationDO row = new CreatorCollaborationDO();
        row.setId(30L);
        row.setTenantId(7L);
        row.setCampaignId(20L);
        row.setCreatorId(10L);
        row.setFixedFeeAmount(new BigDecimal("100.0000"));
        row.setCommissionRate(new BigDecimal("0.100000"));
        row.setStatus("ACTIVE");
        return row;
    }

    private CreatorCollaborationDO otherTenantCollaboration() {
        CreatorCollaborationDO row = collaboration();
        row.setId(31L);
        row.setTenantId(99L);
        row.setFixedFeeAmount(new BigDecimal("999.0000"));
        return row;
    }

    private CreatorDeliverableDO deliverable(String status,
                                             LocalDateTime dueAt,
                                             Long impressions,
                                             Long likes,
                                             Long comments,
                                             Long shares,
                                             Long saves,
                                             Long clicks,
                                             Long conversions,
                                             BigDecimal revenue) {
        CreatorDeliverableDO row = new CreatorDeliverableDO();
        row.setId(100L);
        row.setTenantId(7L);
        row.setCollaborationId(30L);
        row.setCampaignId(20L);
        row.setCreatorId(10L);
        row.setDeliverableKey("video-" + status.toLowerCase());
        row.setContentType("VIDEO");
        row.setDueAt(dueAt);
        row.setStatus(status);
        row.setImpressionCount(impressions);
        row.setLikeCount(likes);
        row.setCommentCount(comments);
        row.setShareCount(shares);
        row.setSaveCount(saves);
        row.setClickCount(clicks);
        row.setConversionCount(conversions);
        row.setRevenueAmount(revenue);
        return row;
    }

    private CreatorDeliverableDO otherTenantDeliverable() {
        CreatorDeliverableDO row = deliverable("POSTED", now().minusDays(1), 9999L, 99L, 99L, 99L, 99L, 99L, 99L,
                new BigDecimal("9999.0000"));
        row.setTenantId(99L);
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
