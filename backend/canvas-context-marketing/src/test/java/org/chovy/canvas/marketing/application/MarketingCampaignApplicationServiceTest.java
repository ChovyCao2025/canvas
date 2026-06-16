package org.chovy.canvas.marketing.application;

import org.chovy.canvas.marketing.api.MarketingCampaignCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkView;
import org.chovy.canvas.marketing.api.MarketingCampaignReadinessView;
import org.chovy.canvas.marketing.api.MarketingCampaignView;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignLink;
import org.chovy.canvas.marketing.domain.MarketingCampaignRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证MarketingCampaignApplicationService的关键兼容行为。
 */
class MarketingCampaignApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 验证 upsert campaign normalizes and inserts tenant scoped record 场景的兼容行为。
     */
    @Test
    void upsertCampaignNormalizesAndInsertsTenantScopedRecord() {
        FakeRepository repository = new FakeRepository();
        MarketingCampaignApplicationService service = new MarketingCampaignApplicationService(repository, CLOCK);

        MarketingCampaignView view = service.upsertCampaign(7L, new MarketingCampaignCommand(
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

        MarketingCampaign saved = repository.campaignsById.get(100L);
        assertThat(saved.tenantId()).isEqualTo(7L);
        assertThat(saved.campaignName()).isEqualTo("Spring launch");
        assertThat(saved.ownerTeam()).isEqualTo("Growth");
        assertThat(saved.budget().amount()).isEqualByComparingTo("1200.50");
        assertThat(saved.createdBy()).isEqualTo("operator-1");
        assertThat(saved.updatedBy()).isEqualTo("operator-1");
    }

    /**
     * 验证 upsert campaign updates existing tenant key and defaults missing values 场景的兼容行为。
     */
    @Test
    void upsertCampaignUpdatesExistingTenantKeyAndDefaultsMissingValues() {
        FakeRepository repository = new FakeRepository();
        MarketingCampaignApplicationService service = new MarketingCampaignApplicationService(repository, CLOCK);
        repository.save(repository.campaign(10L, 7L, "spring-launch", CampaignStatus.ACTIVE));

        MarketingCampaignView view = service.upsertCampaign(7L, new MarketingCampaignCommand(
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
        assertThat(repository.campaignsById.get(10L).updatedBy()).isEqualTo("operator-2");
    }

    /**
     * 验证 rejects invalid dates unsupported status and invalid budget 场景的兼容行为。
     */
    @Test
    void rejectsInvalidDatesUnsupportedStatusAndInvalidBudget() {
        FakeRepository repository = new FakeRepository();
        MarketingCampaignApplicationService service = new MarketingCampaignApplicationService(repository, CLOCK);

        assertThatThrownBy(() -> service.upsertCampaign(7L, new MarketingCampaignCommand(
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

        assertThatThrownBy(() -> service.upsertCampaign(7L, new MarketingCampaignCommand(
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

        assertThatThrownBy(() -> service.upsertCampaign(7L, new MarketingCampaignCommand(
                "spring-launch",
                "Spring launch",
                "acquisition",
                "active",
                null,
                null,
                null,
                null,
                new BigDecimal("-1.00"),
                null,
                Map.of()), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("budgetAmount must be non-negative");
    }

    /**
     * 验证 list campaigns normalizes status and clamps limit 场景的兼容行为。
     */
    @Test
    void listCampaignsNormalizesStatusAndClampsLimit() {
        FakeRepository repository = new FakeRepository();
        MarketingCampaignApplicationService service = new MarketingCampaignApplicationService(repository, CLOCK);
        repository.save(repository.campaign(10L, 7L, "spring-launch", CampaignStatus.ACTIVE));
        repository.save(repository.campaign(11L, 7L, "fall-launch", CampaignStatus.DRAFT));

        List<MarketingCampaignView> rows = service.listCampaigns(7L, "active", 500);

        assertThat(rows).singleElement()
                .satisfies(row -> assertThat(row.status()).isEqualTo("ACTIVE"));
        assertThat(repository.lastListLimit).isEqualTo(200);

        assertThatThrownBy(() -> service.listCampaigns(7L, "launching", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported campaign status");
    }

    /**
     * 验证 link resource validates campaign ownership and inserts required dependency 场景的兼容行为。
     */
    @Test
    void linkResourceValidatesCampaignOwnershipAndInsertsRequiredDependency() {
        FakeRepository repository = new FakeRepository();
        MarketingCampaignApplicationService service = new MarketingCampaignApplicationService(repository, CLOCK);
        repository.save(repository.campaign(10L, 7L, "spring-launch", CampaignStatus.ACTIVE));

        MarketingCampaignLinkView view = service.linkResource(7L, new MarketingCampaignLinkCommand(
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
        assertThat(view.dependencyRole()).isEqualTo("PRIMARY");
        assertThat(view.linkStatus()).isEqualTo("ACTIVE");
        assertThat(view.requiredForLaunch()).isTrue();
        assertThat(view.metadata()).containsEntry("stage", "launch");
    }

    /**
     * 验证 link resource updates existing dependency and rejects foreign campaign 场景的兼容行为。
     */
    @Test
    void linkResourceUpdatesExistingDependencyAndRejectsForeignCampaign() {
        FakeRepository repository = new FakeRepository();
        MarketingCampaignApplicationService service = new MarketingCampaignApplicationService(repository, CLOCK);
        repository.save(repository.campaign(10L, 7L, "spring-launch", CampaignStatus.ACTIVE));
        repository.saveLink(repository.link(20L, 7L, 10L, "CONTENT_RELEASE", "release-v1"));

        MarketingCampaignLinkView view = service.linkResource(7L, new MarketingCampaignLinkCommand(
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

        assertThat(view.id()).isEqualTo(20L);
        assertThat(view.linkStatus()).isEqualTo("BLOCKED");
        assertThat(view.requiredForLaunch()).isFalse();
        assertThat(repository.linksById.get(20L).updatedBy()).isEqualTo("operator-2");

        repository.save(repository.campaign(30L, 8L, "foreign", CampaignStatus.ACTIVE));
        assertThatThrownBy(() -> service.linkResource(7L, new MarketingCampaignLinkCommand(
                30L,
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
    }

    /**
     * 验证 readiness and unlink enforce tenant ownership 场景的兼容行为。
     */
    @Test
    void readinessAndUnlinkEnforceTenantOwnership() {
        FakeRepository repository = new FakeRepository();
        MarketingCampaignApplicationService service = new MarketingCampaignApplicationService(repository, CLOCK);
        repository.save(repository.campaign(10L, 7L, "spring-launch", CampaignStatus.ACTIVE));
        repository.saveLink(repository.link(20L, 7L, 10L, "JOURNEY", "launch-journey"));
        repository.saveLink(repository.link(21L, 7L, 10L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT"));

        MarketingCampaignReadinessView readiness = service.readiness(7L, 10L);

        assertThat(readiness.generatedAt()).isEqualTo("2026-06-06T10:00");
        assertThat(readiness.status()).isEqualTo("READY");

        service.unlinkResource(7L, 20L);
        assertThat(repository.deletedLinkIds).containsExactly(20L);

        repository.saveLink(repository.link(30L, 8L, 10L, "JOURNEY", "foreign"));
        assertThatThrownBy(() -> service.unlinkResource(7L, 30L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campaign link does not belong to tenant");
    }

    /**
     * 定义FakeRepository的数据访问边界。
     */
    private static final class FakeRepository implements MarketingCampaignRepository {
        private final Map<Long, MarketingCampaign> campaignsById = new LinkedHashMap<>();
        private final Map<Long, MarketingCampaignLink> linksById = new LinkedHashMap<>();
        private final List<Long> deletedLinkIds = new ArrayList<>();

        /**
         * 保存nextCampaignId字段值。
         */
        private long nextCampaignId = 100L;

        /**
         * 下一个资源关联内存标识。
         */
        private long nextLinkId = 200L;

        /**
         * 保存lastListLimit字段值。
         */
        private int lastListLimit;

        /**
         * 查找byTenantAndKey业务对象。
         */
        @Override
        public MarketingCampaign findByTenantAndKey(Long tenantId, CampaignKey campaignKey) {
            return campaignsById.values().stream()
                    .filter(campaign -> campaign.tenantId().equals(tenantId))
                    .filter(campaign -> campaign.campaignKey().equals(campaignKey))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * 查找byId业务对象。
         */
        @Override
        public MarketingCampaign findById(Long tenantId, Long campaignId) {
            MarketingCampaign campaign = campaignsById.get(campaignId);
            return campaign != null && campaign.tenantId().equals(tenantId) ? campaign : null;
        }

        /**
         * 执行save业务操作。
         */
        @Override
        public MarketingCampaign save(MarketingCampaign campaign) {
            Long id = campaign.id() == null ? nextCampaignId++ : campaign.id();
            MarketingCampaign saved = campaign.withId(id);
            campaignsById.put(id, saved);
            return saved;
        }

        /**
         * 查询列表。
         */
        @Override
        public List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit) {
            lastListLimit = limit;
            return campaignsById.values().stream()
                    .filter(campaign -> campaign.tenantId().equals(tenantId))
                    .filter(campaign -> status == null || campaign.status() == status)
                    .sorted(Comparator.comparing(MarketingCampaign::campaignKey))
                    .limit(limit)
                    .toList();
        }

        /**
         * 查找link业务对象。
         */
        @Override
        public MarketingCampaignLink findLink(Long tenantId, Long campaignId, String resourceType, CampaignKey resourceKey) {
            return linksById.values().stream()
                    .filter(link -> link.tenantId().equals(tenantId))
                    .filter(link -> link.campaignId().equals(campaignId))
                    .filter(link -> link.resourceType().equals(resourceType))
                    .filter(link -> link.resourceKey().equals(resourceKey))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * 执行saveLink业务操作。
         */
        @Override
        public MarketingCampaignLink saveLink(MarketingCampaignLink link) {
            Long id = link.id() == null ? nextLinkId++ : link.id();
            MarketingCampaignLink saved = link.withId(id);
            linksById.put(id, saved);
            return saved;
        }

        /**
         * 查询links列表。
         */
        @Override
        public List<MarketingCampaignLink> listLinks(Long tenantId, Long campaignId) {
            return linksById.values().stream()
                    .filter(link -> link.tenantId().equals(tenantId))
                    .filter(link -> link.campaignId().equals(campaignId))
                    .sorted(Comparator.comparing(MarketingCampaignLink::resourceType)
                            .thenComparing(link -> link.resourceKey().value()))
                    .toList();
        }

        /**
         * 查找linkById业务对象。
         */
        @Override
        public MarketingCampaignLink findLinkById(Long tenantId, Long linkId) {
            MarketingCampaignLink link = linksById.get(linkId);
            return link != null && link.tenantId().equals(tenantId) ? link : null;
        }

        /**
         * 删除或停用link业务对象。
         */
        @Override
        public void deleteLink(Long tenantId, Long linkId) {
            deletedLinkIds.add(linkId);
            linksById.remove(linkId);
        }

        /**
         * 执行campaign业务操作。
         */
        private MarketingCampaign campaign(Long id, Long tenantId, String key, CampaignStatus status) {
            return MarketingCampaign.createExisting(
                    id,
                    tenantId,
                    CampaignKey.of(key, "campaignKey"),
                    key,
                    "ACQUISITION",
                    status,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.ZERO,
                    "CNY",
                    Map.of(),
                    "operator-1",
                    "operator-1",
                    null,
                    null);
        }

        /**
         * 执行link业务操作。
         */
        private MarketingCampaignLink link(Long id, Long tenantId, Long campaignId, String type, String key) {
            return link(id, tenantId, campaignId, type, key, "PRIMARY");
        }

        /**
         * 执行link业务操作。
         */
        private MarketingCampaignLink link(Long id, Long tenantId, Long campaignId, String type, String key, String role) {
            return MarketingCampaignLink.createExisting(
                    id,
                    tenantId,
                    campaignId,
                    type,
                    300L + id,
                    CampaignKey.of(key, "resourceKey"),
                    key,
                    "/resources/" + key,
                    role,
                    "ACTIVE",
                    true,
                    Map.of(),
                    "operator-1",
                    "operator-1",
                    null,
                    null);
        }
    }
}
