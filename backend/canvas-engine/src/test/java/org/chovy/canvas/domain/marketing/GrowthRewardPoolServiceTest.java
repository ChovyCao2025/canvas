package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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

class GrowthRewardPoolServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-07T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertRewardPoolNormalizesAndInsertsTenantScopedPool() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.poolMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthRewardPoolDO row = invocation.getArgument(0);
            row.setId(100L);
            return 1;
        }).when(harness.poolMapper).insert(any(GrowthRewardPoolDO.class));

        GrowthRewardPoolView view = harness.service.upsertPool(7L, 10L, new GrowthRewardPoolCommand(
                " Coupon Pool 1 ",
                "coupon",
                "commit_action",
                "new-user-20",
                null,
                null,
                "coupon-contract",
                "limited",
                100L,
                1,
                2,
                new BigDecimal("5000.00"),
                "usd",
                "active",
                Map.of("campaign", "spring")), "operator-1");

        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.poolKey()).isEqualTo("coupon-pool-1");
        assertThat(view.rewardType()).isEqualTo("COUPON");
        assertThat(view.grantChannel()).isEqualTo("COMMIT_ACTION");
        assertThat(view.inventoryLow()).isFalse();
        assertThat(view.metadata()).containsEntry("campaign", "spring");
        verify(harness.poolMapper).insert(argThat((GrowthRewardPoolDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityId().equals(10L)
                        && row.getPoolKey().equals("coupon-pool-1")
                        && row.getRewardType().equals("COUPON")
                        && row.getGrantChannel().equals("COMMIT_ACTION")
                        && row.getCouponTypeKey().equals("new-user-20")
                        && row.getInventoryMode().equals("LIMITED")
                        && row.getTotalInventory().equals(100L)
                        && row.getBudgetAmount().compareTo(new BigDecimal("5000.00")) == 0
                        && row.getCostCurrency().equals("USD")
                        && row.getCreatedBy().equals("operator-1")
                        && row.getUpdatedBy().equals("operator-1")));
    }

    @Test
    void listRewardPoolsCalculatesLowInventoryAndRejectsForeignActivity() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.activityMapper.selectById(20L)).thenReturn(activity(20L, 8L));
        when(harness.poolMapper.selectList(any())).thenReturn(List.of(pool(100L, 7L, 10L, "coupon-pool", 3L, "ACTIVE")));

        assertThat(harness.service.listPools(7L, 10L))
                .singleElement()
                .satisfies(pool -> {
                    assertThat(pool.poolKey()).isEqualTo("coupon-pool");
                    assertThat(pool.inventoryLow()).isTrue();
                });

        assertThatThrownBy(() -> harness.service.listPools(7L, 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("growth activity does not belong to tenant");
    }

    @Test
    void upsertRewardPoolUpdatesExistingAndRejectsUnsupportedRewardType() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        GrowthRewardPoolDO existing = pool(100L, 7L, 10L, "coupon-pool", 50L, "ACTIVE");
        when(harness.poolMapper.selectOne(any())).thenReturn(existing);

        GrowthRewardPoolView view = harness.service.upsertPool(7L, 10L, new GrowthRewardPoolCommand(
                "coupon-pool",
                "points",
                "loyalty",
                null,
                "daily-login",
                "BONUS",
                null,
                "unlimited",
                null,
                null,
                null,
                null,
                null,
                "paused",
                Map.of()), "operator-2");

        assertThat(view.rewardType()).isEqualTo("POINTS");
        assertThat(view.status()).isEqualTo("PAUSED");
        verify(harness.poolMapper).updateById(argThat((GrowthRewardPoolDO row) ->
                row.getId().equals(100L)
                        && row.getGrantChannel().equals("LOYALTY")
                        && row.getUpdatedBy().equals("operator-2")));

        assertThatThrownBy(() -> harness.service.upsertPool(7L, 10L, new GrowthRewardPoolCommand(
                "bad",
                "cash",
                "commit_action",
                null,
                null,
                null,
                null,
                "limited",
                1L,
                null,
                null,
                null,
                null,
                "active",
                Map.of()), "operator-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported reward type");
        verify(harness.poolMapper, never()).insert(argThat((GrowthRewardPoolDO row) -> "bad".equals(row.getPoolKey())));
    }

    private static Harness harness() {
        GrowthActivityMapper activityMapper = mock(GrowthActivityMapper.class);
        GrowthRewardPoolMapper poolMapper = mock(GrowthRewardPoolMapper.class);
        return new Harness(activityMapper, poolMapper,
                new GrowthRewardPoolService(activityMapper, poolMapper, new ObjectMapper(), CLOCK));
    }

    private static GrowthActivityDO activity(Long id, Long tenantId) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityKey("invite-spring");
        row.setStatus("ACTIVE");
        return row;
    }

    private static GrowthRewardPoolDO pool(Long id, Long tenantId, Long activityId, String key, Long totalInventory, String status) {
        GrowthRewardPoolDO row = new GrowthRewardPoolDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setPoolKey(key);
        row.setRewardType("COUPON");
        row.setGrantChannel("COMMIT_ACTION");
        row.setInventoryMode("LIMITED");
        row.setTotalInventory(totalInventory);
        row.setPerUserLimit(1);
        row.setBudgetAmount(BigDecimal.ZERO);
        row.setCostCurrency("CNY");
        row.setStatus(status);
        row.setMetadataJson("{}");
        row.setCreatedBy("operator-1");
        row.setUpdatedBy("operator-1");
        return row;
    }

    private record Harness(
            GrowthActivityMapper activityMapper,
            GrowthRewardPoolMapper poolMapper,
            GrowthRewardPoolService service) {
    }
}
