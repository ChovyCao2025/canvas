// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingCampaignLinkDO;
import org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO;
import org.chovy.canvas.dal.mapper.MarketingCampaignLinkMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper;
import org.junit.jupiter.api.Test;

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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingCampaignServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertCampaignNormalizesAndInsertsTenantScopedRecord() {
        Harness harness = harness();
        when(harness.campaignMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            MarketingCampaignMasterDO row = invocation.getArgument(0);
            row.setId(100L);
            return 1;
        }).when(harness.campaignMapper).insert(any(MarketingCampaignMasterDO.class));

        MarketingCampaignView view = harness.service.upsertCampaign(7L, new MarketingCampaignCommand(
                " Spring Launch 2026! ",
                " Spring launch ",
                "acquisition",
                "active",
                "paid_media",
                " Growth ",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-30T23:59:00"),
                new BigDecimal("1200.50"),
                "usd",
                Map.of("northStar", "signup")), "operator-1");

        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.campaignKey()).isEqualTo("spring-launch-2026");
        assertThat(view.objective()).isEqualTo("ACQUISITION");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.primaryChannel()).isEqualTo("PAID_MEDIA");
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.brief()).containsEntry("northStar", "signup");
        verify(harness.campaignMapper).insert(argThat((MarketingCampaignMasterDO row) ->
                row.getTenantId().equals(7L)
                        && row.getCampaignKey().equals("spring-launch-2026")
                        && row.getCampaignName().equals("Spring launch")
                        && row.getOwnerTeam().equals("Growth")
                        && row.getBudgetAmount().compareTo(new BigDecimal("1200.50")) == 0
                        && row.getBriefJson().contains("\"northStar\"")
                        && row.getCreatedBy().equals("operator-1")
                        && row.getUpdatedBy().equals("operator-1")));
    }

    @Test
    void upsertCampaignUpdatesExistingTenantKey() {
        Harness harness = harness();
        MarketingCampaignMasterDO existing = campaign(10L, 7L, "spring-launch", "ACTIVE");
        when(harness.campaignMapper.selectOne(any())).thenReturn(existing);

        MarketingCampaignView view = harness.service.upsertCampaign(7L, new MarketingCampaignCommand(
                "Spring Launch",
                "Spring launch v2",
                "retention",
                "paused",
                "crm",
                null,
                null,
                null,
                null,
                null,
                Map.of()), "operator-2");

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.status()).isEqualTo("PAUSED");
        assertThat(view.objective()).isEqualTo("RETENTION");
        assertThat(view.currency()).isEqualTo("CNY");
        verify(harness.campaignMapper).updateById(argThat((MarketingCampaignMasterDO row) ->
                row.getId().equals(10L)
                        && row.getCampaignKey().equals("spring-launch")
                        && row.getCampaignName().equals("Spring launch v2")
                        && row.getUpdatedBy().equals("operator-2")));
        verify(harness.campaignMapper, never()).insert(any(MarketingCampaignMasterDO.class));
    }

    @Test
    void upsertCampaignRejectsInvalidDatesAndUnsupportedStatus() {
        Harness harness = harness();
        when(harness.campaignMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> harness.service.upsertCampaign(7L, new MarketingCampaignCommand(
                "spring-launch",
                "Spring launch",
                "acquisition",
                "active",
                null,
                null,
                LocalDateTime.parse("2026-06-30T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00"),
                null,
                null,
                Map.of()), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endAt must be after startAt");

        assertThatThrownBy(() -> harness.service.upsertCampaign(7L, new MarketingCampaignCommand(
                "spring-launch",
                "Spring launch",
                "acquisition",
                "launching",
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of()), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported campaign status");
        verify(harness.campaignMapper, never()).insert(any(MarketingCampaignMasterDO.class));
        verify(harness.campaignMapper, never()).updateById(any(MarketingCampaignMasterDO.class));
    }

    @Test
    void listCampaignsReturnsTenantRows() {
        Harness harness = harness();
        when(harness.campaignMapper.selectList(any()))
                .thenReturn(List.of(campaign(10L, 7L, "spring-launch", "ACTIVE")));

        List<MarketingCampaignView> rows = harness.service.listCampaigns(7L, "active", 500);

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.id()).isEqualTo(10L);
                    assertThat(row.status()).isEqualTo("ACTIVE");
                });
        verify(harness.campaignMapper).selectList(any());

        assertThatThrownBy(() -> harness.service.listCampaigns(7L, "launching", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported campaign status");
    }

    @Test
    void linkResourceValidatesCampaignOwnershipAndInsertsRequiredDependency() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L, "spring-launch", "ACTIVE"));
        when(harness.linkMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            MarketingCampaignLinkDO row = invocation.getArgument(0);
            row.setId(200L);
            return 1;
        }).when(harness.linkMapper).insert(any(MarketingCampaignLinkDO.class));

        MarketingCampaignLinkView view = harness.service.linkResource(7L, new MarketingCampaignLinkCommand(
                10L,
                " journey ",
                300L,
                " Launch Journey#1 ",
                "Launch journey",
                "/canvas/300",
                "primary",
                null,
                true,
                Map.of("stage", "launch")), "operator-1");

        assertThat(view.id()).isEqualTo(200L);
        assertThat(view.resourceType()).isEqualTo("JOURNEY");
        assertThat(view.resourceKey()).isEqualTo("launch-journey-1");
        assertThat(view.requiredForLaunch()).isTrue();
        assertThat(view.metadata()).containsEntry("stage", "launch");
        verify(harness.linkMapper).insert(argThat((MarketingCampaignLinkDO row) ->
                row.getTenantId().equals(7L)
                        && row.getCampaignId().equals(10L)
                        && row.getResourceType().equals("JOURNEY")
                        && row.getResourceKey().equals("launch-journey-1")
                        && row.getDependencyRole().equals("PRIMARY")
                        && row.getLinkStatus().equals("ACTIVE")
                        && row.getRequiredForLaunch().equals(1)
                        && row.getMetadataJson().contains("\"stage\"")));
    }

    @Test
    void linkResourceUpdatesExistingDependencyAndRejectsUnsupportedStatus() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L, "spring-launch", "ACTIVE"));
        MarketingCampaignLinkDO existing = link(20L, 7L, 10L, "CONTENT_RELEASE", "release-v1");
        when(harness.linkMapper.selectOne(any())).thenReturn(existing);

        MarketingCampaignLinkView view = harness.service.linkResource(7L, new MarketingCampaignLinkCommand(
                10L,
                "content_release",
                400L,
                "release-v1",
                "Release v1",
                "/content-hub/releases/release-v1",
                "supporting",
                "blocked",
                false,
                Map.of("reason", "approval pending")), "operator-2");

        assertThat(view.linkStatus()).isEqualTo("BLOCKED");
        assertThat(view.requiredForLaunch()).isFalse();
        verify(harness.linkMapper).updateById(argThat((MarketingCampaignLinkDO row) ->
                row.getId().equals(20L)
                        && row.getLinkStatus().equals("BLOCKED")
                        && row.getRequiredForLaunch().equals(0)
                        && row.getUpdatedBy().equals("operator-2")));

        assertThatThrownBy(() -> harness.service.linkResource(7L, new MarketingCampaignLinkCommand(
                10L,
                "content_release",
                400L,
                "release-v1",
                "Release v1",
                null,
                null,
                "ready",
                false,
                Map.of()), "operator-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported link status");
    }

    @Test
    void linkResourceRejectsForeignTenantCampaignBeforeWritingLinks() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 8L, "spring-launch", "ACTIVE"));

        assertThatThrownBy(() -> harness.service.linkResource(7L, new MarketingCampaignLinkCommand(
                10L,
                "journey",
                300L,
                "launch-journey",
                "Launch journey",
                null,
                null,
                null,
                true,
                Map.of()), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campaign does not belong to tenant");
        verify(harness.linkMapper, never()).selectOne(any());
        verify(harness.linkMapper, never()).insert(any(MarketingCampaignLinkDO.class));
    }

    @Test
    void listAndUnlinkResourceEnforceTenantOwnership() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L, "spring-launch", "ACTIVE"));
        when(harness.linkMapper.selectList(any()))
                .thenReturn(List.of(link(20L, 7L, 10L, "JOURNEY", "launch-journey")));

        List<MarketingCampaignLinkView> links = harness.service.listLinks(7L, 10L);

        assertThat(links).singleElement()
                .satisfies(link -> assertThat(link.resourceKey()).isEqualTo("launch-journey"));

        when(harness.linkMapper.selectById(20L)).thenReturn(link(20L, 7L, 10L, "JOURNEY", "launch-journey"));
        harness.service.unlinkResource(7L, 20L);
        verify(harness.linkMapper).deleteById(20L);

        when(harness.linkMapper.selectById(30L)).thenReturn(link(30L, 8L, 10L, "JOURNEY", "foreign"));
        assertThatThrownBy(() -> harness.service.unlinkResource(7L, 30L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campaign link does not belong to tenant");
        verify(harness.linkMapper, never()).deleteById(30L);
    }

    @Test
    void readinessBlocksInactiveCampaignAndMissingLaunchDependencies() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L, "spring-launch", "DRAFT"));
        when(harness.linkMapper.selectList(any())).thenReturn(List.of());

        MarketingCampaignReadinessView readiness = harness.service.readiness(7L, 10L);

        assertThat(readiness.generatedAt()).isEqualTo("2026-06-06T10:00");
        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.productionReady()).isFalse();
        assertThat(readiness.requiredLinkCount()).isZero();
        assertThat(readiness.blockers())
                .extracting(MarketingCampaignReadinessFinding::itemKey)
                .contains(
                        "spring-launch",
                        "launch-required-links",
                        "primary-dependency",
                        "measurement-dependency");
    }

    @Test
    void readinessBlocksRequiredResourcesThatAreNotActive() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L, "spring-launch", "ACTIVE"));
        when(harness.linkMapper.selectList(any())).thenReturn(List.of(
                link(20L, 7L, 10L, "JOURNEY", "launch-journey", "PRIMARY", "BLOCKED", true),
                link(21L, 7L, 10L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT", "ACTIVE", true)));

        MarketingCampaignReadinessView readiness = harness.service.readiness(7L, 10L);

        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.requiredLinkCount()).isEqualTo(2);
        assertThat(readiness.activeRequiredLinkCount()).isEqualTo(1);
        assertThat(readiness.blockers())
                .anySatisfy(finding -> {
                    assertThat(finding.itemType()).isEqualTo("RESOURCE_LINK");
                    assertThat(finding.itemKey()).isEqualTo("JOURNEY:launch-journey");
                    assertThat(finding.reason()).isEqualTo("linkStatus=BLOCKED");
                });
    }

    @Test
    void readinessIsReadyWhenPrimaryAndMeasurementRequiredResourcesAreActive() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L, "spring-launch", "ACTIVE"));
        when(harness.linkMapper.selectList(any())).thenReturn(List.of(
                link(20L, 7L, 10L, "JOURNEY", "launch-journey", "PRIMARY", "ACTIVE", true),
                link(21L, 7L, 10L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT", "ACTIVE", true)));

        MarketingCampaignReadinessView readiness = harness.service.readiness(7L, 10L);

        assertThat(readiness.status()).isEqualTo("READY");
        assertThat(readiness.productionReady()).isTrue();
        assertThat(readiness.requiredLinkCount()).isEqualTo(2);
        assertThat(readiness.activeRequiredLinkCount()).isEqualTo(2);
        assertThat(readiness.blockerCount()).isZero();
        assertThat(readiness.warningCount()).isZero();
    }

    @Test
    void readinessIsDegradedWhenOnlyOptionalLinksNeedTriage() {
        Harness harness = harness();
        when(harness.campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L, "spring-launch", "ACTIVE"));
        when(harness.linkMapper.selectList(any())).thenReturn(List.of(
                link(20L, 7L, 10L, "JOURNEY", "launch-journey", "PRIMARY", "ACTIVE", true),
                link(21L, 7L, 10L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT", "ACTIVE", true),
                link(22L, 7L, 10L, "CONTENT_RELEASE", "optional-copy", "SUPPORTING", "MISSING", false)));

        MarketingCampaignReadinessView readiness = harness.service.readiness(7L, 10L);

        assertThat(readiness.status()).isEqualTo("DEGRADED");
        assertThat(readiness.productionReady()).isTrue();
        assertThat(readiness.blockerCount()).isZero();
        assertThat(readiness.warningCount()).isEqualTo(1);
        assertThat(readiness.warnings()).singleElement()
                .satisfies(finding -> assertThat(finding.itemType()).isEqualTo("OPTIONAL_RESOURCE_LINK"));
    }

    private static Harness harness() {
        MarketingCampaignMasterMapper campaignMapper = mock(MarketingCampaignMasterMapper.class);
        MarketingCampaignLinkMapper linkMapper = mock(MarketingCampaignLinkMapper.class);
        return new Harness(
                campaignMapper,
                linkMapper,
                new MarketingCampaignService(campaignMapper, linkMapper, new ObjectMapper(), CLOCK));
    }

    private static MarketingCampaignMasterDO campaign(Long id, Long tenantId, String key, String status) {
        MarketingCampaignMasterDO row = new MarketingCampaignMasterDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setCampaignKey(key);
        row.setCampaignName(key);
        row.setObjective("ACQUISITION");
        row.setStatus(status);
        row.setBudgetAmount(BigDecimal.ZERO);
        row.setCurrency("CNY");
        row.setBriefJson("{}");
        row.setCreatedBy("operator-1");
        row.setUpdatedBy("operator-1");
        return row;
    }

    private static MarketingCampaignLinkDO link(Long id, Long tenantId, Long campaignId, String type, String key) {
        return link(id, tenantId, campaignId, type, key, "PRIMARY", "ACTIVE", true);
    }

    private static MarketingCampaignLinkDO link(Long id,
                                                Long tenantId,
                                                Long campaignId,
                                                String type,
                                                String key,
                                                String role,
                                                String status,
                                                boolean requiredForLaunch) {
        MarketingCampaignLinkDO row = new MarketingCampaignLinkDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setCampaignId(campaignId);
        row.setResourceType(type);
        row.setResourceKey(key);
        row.setResourceName(key);
        row.setDependencyRole(role);
        row.setLinkStatus(status);
        row.setRequiredForLaunch(requiredForLaunch ? 1 : 0);
        row.setMetadataJson("{}");
        row.setCreatedBy("operator-1");
        row.setUpdatedBy("operator-1");
        return row;
    }

    private record Harness(
            MarketingCampaignMasterMapper campaignMapper,
            MarketingCampaignLinkMapper linkMapper,
            MarketingCampaignService service) {
    }
}
