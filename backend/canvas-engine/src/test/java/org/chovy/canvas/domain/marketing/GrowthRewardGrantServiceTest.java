package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper;
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

class GrowthRewardGrantServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-07T03:30:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void createGrantInsertsReservedGrantWithTenantScopedIdempotency() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.poolMapper.selectById(100L)).thenReturn(pool(100L, 7L, 10L, "ACTIVE"));
        when(harness.grantMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthRewardGrantDO row = invocation.getArgument(0);
            row.setId(300L);
            return 1;
        }).when(harness.grantMapper).insert(any(GrowthRewardGrantDO.class));

        GrowthRewardGrantView view = harness.service.createGrant(7L, 10L, new GrowthRewardGrantCommand(
                100L,
                200L,
                null,
                null,
                "QUALIFIED_REFERRAL",
                "invite-spring:inviter-1:invitee-1",
                Map.of("couponTypeKey", "new-user-20"),
                new BigDecimal("20.00")), "operator-1");

        assertThat(view.id()).isEqualTo(300L);
        assertThat(view.status()).isEqualTo("RESERVED");
        assertThat(view.idempotencyKey()).isEqualTo("invite-spring:inviter-1:invitee-1");
        assertThat(view.providerRequest()).containsEntry("couponTypeKey", "new-user-20");
        verify(harness.grantMapper).insert(argThat((GrowthRewardGrantDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityId().equals(10L)
                        && row.getPoolId().equals(100L)
                        && row.getParticipantId().equals(200L)
                        && row.getGrantReason().equals("QUALIFIED_REFERRAL")
                        && row.getStatus().equals("RESERVED")
                        && row.getIdempotencyKey().equals("invite-spring:inviter-1:invitee-1")
                        && row.getCostAmount().compareTo(new BigDecimal("20.00")) == 0
                        && row.getCreatedBy().equals("operator-1")
                        && row.getUpdatedBy().equals("operator-1")));
    }

    @Test
    void createGrantReturnsExistingGrantForDuplicateIdempotencyKey() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.poolMapper.selectById(100L)).thenReturn(pool(100L, 7L, 10L, "ACTIVE"));
        when(harness.grantMapper.selectOne(any())).thenReturn(grant(300L, 7L, 10L, 100L, "idem-1", "SUCCESS"));

        GrowthRewardGrantView view = harness.service.createGrant(7L, 10L, new GrowthRewardGrantCommand(
                100L,
                200L,
                null,
                null,
                "QUALIFIED_REFERRAL",
                "idem-1",
                Map.of("couponTypeKey", "new-user-20"),
                BigDecimal.ZERO), "operator-1");

        assertThat(view.id()).isEqualTo(300L);
        assertThat(view.status()).isEqualTo("SUCCESS");
        verify(harness.grantMapper, never()).insert(any(GrowthRewardGrantDO.class));
    }

    @Test
    void createGrantPreConsumesInventoryAndBudgetCounters() {
        Harness harness = harness();
        GrowthRewardPoolDO pool = pool(100L, 7L, 10L, "ACTIVE");
        pool.setTotalInventory(2L);
        pool.setGrantedInventory(1L);
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.poolMapper.selectById(100L)).thenReturn(pool);
        when(harness.grantMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthRewardGrantDO row = invocation.getArgument(0);
            row.setId(300L);
            return 1;
        }).when(harness.grantMapper).insert(any(GrowthRewardGrantDO.class));

        harness.service.createGrant(7L, 10L, new GrowthRewardGrantCommand(
                100L,
                200L,
                null,
                null,
                "QUALIFIED_REFERRAL",
                "idem-reserve",
                Map.of("couponTypeKey", "new-user-20"),
                new BigDecimal("20.00")), "operator-1");

        verify(harness.poolMapper).updateById(argThat((GrowthRewardPoolDO row) ->
                row.getId().equals(100L)
                        && row.getReservedInventory().equals(1L)
                        && row.getGrantedInventory().equals(1L)
                        && row.getReservedAmount().compareTo(new BigDecimal("20.00")) == 0
                        && row.getUpdatedBy().equals("operator-1")));
    }

    @Test
    void createGrantRejectsExhaustedLimitedInventory() {
        Harness harness = harness();
        GrowthRewardPoolDO pool = pool(100L, 7L, 10L, "ACTIVE");
        pool.setTotalInventory(1L);
        pool.setGrantedInventory(1L);
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.poolMapper.selectById(100L)).thenReturn(pool);
        when(harness.grantMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> harness.service.createGrant(7L, 10L, new GrowthRewardGrantCommand(
                100L,
                200L,
                null,
                null,
                "QUALIFIED_REFERRAL",
                "idem-exhausted",
                Map.of(),
                BigDecimal.ONE), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reward pool inventory exhausted");
        verify(harness.grantMapper, never()).insert(any(GrowthRewardGrantDO.class));
    }

    @Test
    void markSuccessMovesReservedCountersToGranted() {
        Harness harness = harness();
        GrowthRewardGrantDO grant = grant(300L, 7L, 10L, 100L, "idem-1", "RESERVED");
        grant.setCostAmount(new BigDecimal("20.00"));
        GrowthRewardPoolDO pool = pool(100L, 7L, 10L, "ACTIVE");
        pool.setReservedInventory(1L);
        pool.setReservedAmount(new BigDecimal("20.00"));
        when(harness.grantMapper.selectById(300L)).thenReturn(grant);
        when(harness.poolMapper.selectById(100L)).thenReturn(pool);

        assertThat(harness.service.markSuccess(7L, 300L, Map.of("couponId", "c-1"), "operator-2").status())
                .isEqualTo("SUCCESS");

        verify(harness.poolMapper).updateById(argThat((GrowthRewardPoolDO row) ->
                row.getId().equals(100L)
                        && row.getReservedInventory().equals(0L)
                        && row.getGrantedInventory().equals(1L)
                        && row.getReservedAmount().compareTo(BigDecimal.ZERO) == 0
                        && row.getGrantedAmount().compareTo(new BigDecimal("20.00")) == 0));
    }

    @Test
    void markGrantSuccessFailureAndCancelValidateTenantAndStatus() {
        Harness harness = harness();
        when(harness.grantMapper.selectById(300L)).thenReturn(grant(300L, 7L, 10L, 100L, "idem-1", "RESERVED"));

        assertThat(harness.service.markSuccess(7L, 300L, Map.of("couponId", "c-1"), "operator-2").status()).isEqualTo("SUCCESS");
        verify(harness.grantMapper).updateById(argThat((GrowthRewardGrantDO row) ->
                row.getStatus().equals("SUCCESS")
                        && row.getProviderResponseJson().contains("\"couponId\"")
                        && row.getUpdatedBy().equals("operator-2")));

        when(harness.grantMapper.selectById(301L)).thenReturn(grant(301L, 7L, 10L, 100L, "idem-2", "RESERVED"));
        assertThat(harness.service.markFailure(7L, 301L, Map.of("message", "provider down"), "operator-3").status()).isEqualTo("FAILED");

        when(harness.grantMapper.selectById(302L)).thenReturn(grant(302L, 8L, 10L, 100L, "idem-3", "RESERVED"));
        assertThatThrownBy(() -> harness.service.cancelGrant(7L, 302L, "operator-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reward grant does not belong to tenant");
    }

    @Test
    void markRedeemedAndExpiredApplyTerminalStatuses() {
        Harness harness = harness();
        when(harness.grantMapper.selectById(300L)).thenReturn(grant(300L, 7L, 10L, 100L, "idem-1", "SUCCESS"));
        when(harness.grantMapper.selectById(301L)).thenReturn(grant(301L, 7L, 10L, 100L, "idem-2", "SUCCESS"));

        assertThat(harness.service.markRedeemed(7L, 300L, Map.of("redeemedAt", "2026-06-07T12:00:00"), "operator-5")
                .status()).isEqualTo("REDEEMED");
        assertThat(harness.service.markExpired(7L, 301L, Map.of("expiredAt", "2026-06-08T00:00:00"), "operator-6")
                .status()).isEqualTo("EXPIRED");
    }

    @Test
    void reconcileGrantUpdatesNonTerminalProviderState() {
        Harness harness = harness();
        when(harness.grantMapper.selectById(300L)).thenReturn(grant(300L, 7L, 10L, 100L, "idem-1", "FAILED"));

        GrowthRewardGrantView view = harness.service.reconcileGrant(
                7L,
                300L,
                "success",
                Map.of("providerGrantId", "pg-1"),
                "operator-7");

        assertThat(view.status()).isEqualTo("SUCCESS");
        verify(harness.grantMapper).updateById(argThat((GrowthRewardGrantDO row) ->
                row.getStatus().equals("SUCCESS")
                        && row.getProviderResponseJson().contains("\"providerGrantId\"")
                        && row.getUpdatedBy().equals("operator-7")));
    }

    @Test
    void listGrantsUseTenantAndActivityScope() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.grantMapper.selectList(any())).thenReturn(List.of(
                grant(300L, 7L, 10L, 100L, "task:900:completion", "FAILED")));

        List<GrowthRewardGrantView> grants = harness.service.listGrants(7L, 10L);

        assertThat(grants).singleElement().satisfies(grant -> {
            assertThat(grant.idempotencyKey()).isEqualTo("task:900:completion");
            assertThat(grant.status()).isEqualTo("FAILED");
        });
        verify(harness.grantMapper).selectList(any());
    }

    private static Harness harness() {
        GrowthActivityMapper activityMapper = mock(GrowthActivityMapper.class);
        GrowthRewardPoolMapper poolMapper = mock(GrowthRewardPoolMapper.class);
        GrowthRewardGrantMapper grantMapper = mock(GrowthRewardGrantMapper.class);
        return new Harness(activityMapper, poolMapper, grantMapper,
                new GrowthRewardGrantService(activityMapper, poolMapper, grantMapper, new ObjectMapper(), CLOCK));
    }

    private static GrowthActivityDO activity(Long id, Long tenantId) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setStatus("ACTIVE");
        return row;
    }

    private static GrowthRewardPoolDO pool(Long id, Long tenantId, Long activityId, String status) {
        GrowthRewardPoolDO row = new GrowthRewardPoolDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setPoolKey("coupon-pool");
        row.setRewardType("COUPON");
        row.setGrantChannel("COMMIT_ACTION");
        row.setInventoryMode("LIMITED");
        row.setTotalInventory(10L);
        row.setReservedInventory(0L);
        row.setGrantedInventory(0L);
        row.setReservedAmount(BigDecimal.ZERO);
        row.setGrantedAmount(BigDecimal.ZERO);
        row.setStatus(status);
        row.setMetadataJson("{}");
        return row;
    }

    private static GrowthRewardGrantDO grant(Long id, Long tenantId, Long activityId, Long poolId, String idempotencyKey, String status) {
        GrowthRewardGrantDO row = new GrowthRewardGrantDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setPoolId(poolId);
        row.setGrantReason("QUALIFIED_REFERRAL");
        row.setStatus(status);
        row.setIdempotencyKey(idempotencyKey);
        row.setProviderRequestJson("{}");
        row.setProviderResponseJson("{}");
        row.setCostAmount(BigDecimal.ZERO);
        row.setCreatedBy("operator-1");
        row.setUpdatedBy("operator-1");
        return row;
    }

    private record Harness(
            GrowthActivityMapper activityMapper,
            GrowthRewardPoolMapper poolMapper,
            GrowthRewardGrantMapper grantMapper,
            GrowthRewardGrantService service) {
    }
}
