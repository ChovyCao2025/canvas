package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GrowthActivityReadinessServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-07T07:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void evaluateReturnsReadyWhenLaunchDependenciesAreSatisfied() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(readyActivity());
        when(harness.poolMapper.selectList(any())).thenReturn(List.of(pool("coupon-provider", "ACTIVE", "100.00")));
        when(harness.contractMapper.selectList(any())).thenReturn(List.of(contract("coupon-provider", "ACTIVE", "PRODUCTION")));
        when(harness.grantMapper.selectList(any())).thenReturn(List.of(
                grant("FAILED"),
                grant("SUCCESS")));

        GrowthActivityReadinessView view = harness.service.evaluate(7L, 10L);

        assertThat(view.status()).isEqualTo("READY");
        assertThat(view.productionReady()).isTrue();
        assertThat(view.blockerCount()).isZero();
        assertThat(view.warningCount()).isZero();
        assertThat(view.checks()).extracting(GrowthActivityReadinessCheckView::itemKey)
                .contains("campaign-master", "journey-link", "reward-pool", "provider-contract",
                        "audience-availability", "analytics-link", "failed-grant-threshold");
    }

    @Test
    void evaluateReportsBlockersAndFailedGrantThresholdWarning() {
        Harness harness = harness();
        GrowthActivityDO activity = activity(10L, 7L);
        activity.setCampaignId(null);
        activity.setActivityType("REFERRAL_INVITE");
        activity.setAudienceRefsJson("{}");
        activity.setRiskPolicyRef(null);
        activity.setDashboardRef(null);
        activity.setMetadataJson("{\"failedGrantThreshold\":2}");
        when(harness.activityMapper.selectById(10L)).thenReturn(activity);
        when(harness.poolMapper.selectList(any())).thenReturn(List.of(pool("missing-contract", "ACTIVE", "0.00")));
        when(harness.contractMapper.selectList(any())).thenReturn(List.of(contract("missing-contract", "PAUSED", "PRODUCTION")));
        when(harness.grantMapper.selectList(any())).thenReturn(List.of(
                grant("FAILED"),
                grant("FAILED"),
                grant("SUCCESS")));

        GrowthActivityReadinessView view = harness.service.evaluate(7L, 10L);

        assertThat(view.status()).isEqualTo("BLOCKED");
        assertThat(view.productionReady()).isFalse();
        assertThat(view.blockers()).extracting(GrowthActivityReadinessCheckView::itemKey)
                .contains("campaign-master", "journey-link", "reward-budget", "provider-contract",
                        "risk-policy", "audience-availability", "analytics-link");
        assertThat(view.warnings()).extracting(GrowthActivityReadinessCheckView::itemKey)
                .contains("failed-grant-threshold");
    }

    @Test
    void evaluateRejectsForeignActivity() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 8L));

        assertThatThrownBy(() -> harness.service.evaluate(7L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("growth activity does not belong to tenant");
    }

    private static Harness harness() {
        GrowthActivityMapper activityMapper = mock(GrowthActivityMapper.class);
        GrowthRewardPoolMapper poolMapper = mock(GrowthRewardPoolMapper.class);
        GrowthRewardGrantMapper grantMapper = mock(GrowthRewardGrantMapper.class);
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        return new Harness(activityMapper, poolMapper, grantMapper, contractMapper,
                new GrowthActivityReadinessService(
                        activityMapper,
                        poolMapper,
                        grantMapper,
                        contractMapper,
                        new ObjectMapper(),
                        CLOCK));
    }

    private static GrowthActivityDO readyActivity() {
        GrowthActivityDO row = activity(10L, 7L);
        row.setCampaignId(100L);
        row.setActivityType("REFERRAL_INVITE");
        row.setAudienceRefsJson("{\"segmentIds\":[\"seg-1\"]}");
        row.setRiskPolicyRef("risk-referral-basic");
        row.setDashboardRef("dashboard-growth-10");
        row.setMetadataJson("{\"journeyRef\":\"canvas-20\",\"contentReleaseRef\":\"content-release-30\",\"failedGrantThreshold\":3}");
        return row;
    }

    private static GrowthActivityDO activity(Long id, Long tenantId) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityKey("invite-spring");
        row.setActivityName("Invite spring");
        row.setActivityType("BENEFIT_PROMOTION");
        row.setStatus("DRAFT");
        row.setAudienceRefsJson("{}");
        row.setMetadataJson("{}");
        return row;
    }

    private static GrowthRewardPoolDO pool(String contractKey, String status, String budget) {
        GrowthRewardPoolDO row = new GrowthRewardPoolDO();
        row.setId(200L);
        row.setTenantId(7L);
        row.setActivityId(10L);
        row.setPoolKey("coupon-pool");
        row.setStatus(status);
        row.setExternalContractKey(contractKey);
        row.setBudgetAmount(new BigDecimal(budget));
        row.setInventoryMode("LIMITED");
        row.setTotalInventory(100L);
        row.setReservedInventory(1L);
        row.setGrantedInventory(2L);
        return row;
    }

    private static MarketingIntegrationContractDO contract(String key, String status, String environment) {
        MarketingIntegrationContractDO row = new MarketingIntegrationContractDO();
        row.setId(300L);
        row.setTenantId(7L);
        row.setContractKey(key);
        row.setStatus(status);
        row.setEnvironment(environment);
        return row;
    }

    private static GrowthRewardGrantDO grant(String status) {
        GrowthRewardGrantDO row = new GrowthRewardGrantDO();
        row.setTenantId(7L);
        row.setActivityId(10L);
        row.setStatus(status);
        return row;
    }

    private record Harness(
            GrowthActivityMapper activityMapper,
            GrowthRewardPoolMapper poolMapper,
            GrowthRewardGrantMapper grantMapper,
            MarketingIntegrationContractMapper contractMapper,
            GrowthActivityReadinessService service) {
    }
}
