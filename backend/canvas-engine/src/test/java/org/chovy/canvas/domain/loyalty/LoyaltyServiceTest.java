package org.chovy.canvas.domain.loyalty;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.dal.dataobject.LoyaltyMemberAccountDO;
import org.chovy.canvas.dal.dataobject.LoyaltyRedemptionDO;
import org.chovy.canvas.dal.dataobject.LoyaltyRuleDO;
import org.chovy.canvas.dal.dataobject.LoyaltyTransactionJournalDO;
import org.chovy.canvas.dal.mapper.LoyaltyMemberAccountMapper;
import org.chovy.canvas.dal.mapper.LoyaltyRedemptionMapper;
import org.chovy.canvas.dal.mapper.LoyaltyRuleMapper;
import org.chovy.canvas.dal.mapper.LoyaltyTransactionJournalMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoyaltyServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-06T03:00:00Z"), ZoneId.of("UTC"));

    @Test
    void earnCreatesAccountJournalAndAdvancesTierByLifetimePoints() {
        LoyaltyMemberAccountMapper accountMapper = mock(LoyaltyMemberAccountMapper.class);
        LoyaltyTransactionJournalMapper journalMapper = mock(LoyaltyTransactionJournalMapper.class);
        LoyaltyService service = service(accountMapper, journalMapper,
                mock(LoyaltyRedemptionMapper.class), mock(LoyaltyRuleMapper.class));
        when(accountMapper.insert(any(LoyaltyMemberAccountDO.class))).thenAnswer(invocation -> {
            LoyaltyMemberAccountDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        });

        LoyaltyService.LoyaltyAccountView view = service.earn(7L, "user-1", new LoyaltyService.EarnCommand(
                "earn-order-1",
                1200,
                "BASE",
                "ORDER",
                "order-1",
                "paid order",
                null));

        assertThat(view.accountId()).isEqualTo(101L);
        assertThat(view.pointsBalance()).isEqualTo(1200);
        assertThat(view.lifetimePoints()).isEqualTo(1200);
        assertThat(view.tierCode()).isEqualTo("GOLD");
        ArgumentCaptor<LoyaltyMemberAccountDO> accountCaptor = ArgumentCaptor.forClass(LoyaltyMemberAccountDO.class);
        verify(accountMapper).insert(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(accountCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(accountCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        ArgumentCaptor<LoyaltyTransactionJournalDO> journalCaptor =
                ArgumentCaptor.forClass(LoyaltyTransactionJournalDO.class);
        verify(journalMapper).insert(journalCaptor.capture());
        assertThat(journalCaptor.getValue().getTransactionKey()).isEqualTo("earn-order-1");
        assertThat(journalCaptor.getValue().getPointsDelta()).isEqualTo(1200);
        assertThat(journalCaptor.getValue().getBalanceAfter()).isEqualTo(1200);
    }

    @Test
    void redemptionIsIdempotentByRedemptionKey() {
        LoyaltyMemberAccountMapper accountMapper = mock(LoyaltyMemberAccountMapper.class);
        LoyaltyRedemptionMapper redemptionMapper = mock(LoyaltyRedemptionMapper.class);
        LoyaltyRedemptionDO existing = redemptionRow("redeem-1", "REDEEMED", null);
        when(redemptionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        LoyaltyService service = service(accountMapper, mock(LoyaltyTransactionJournalMapper.class),
                redemptionMapper, mock(LoyaltyRuleMapper.class));

        LoyaltyService.RedemptionView view = service.redeem(7L, "user-1", new LoyaltyService.RedemptionCommand(
                "redeem-1",
                "coupon-20",
                200,
                "coupon redemption"));

        assertThat(view.status()).isEqualTo("REDEEMED");
        assertThat(view.redemptionKey()).isEqualTo("redeem-1");
        verify(accountMapper, never()).updateById(any(LoyaltyMemberAccountDO.class));
        verify(redemptionMapper, never()).insert(any(LoyaltyRedemptionDO.class));
    }

    @Test
    void redemptionRejectsInsufficientBalanceAndPersistsFailedAttempt() {
        LoyaltyMemberAccountDO account = account("user-1", 50, 50, "BASIC");
        LoyaltyMemberAccountMapper accountMapper = mock(LoyaltyMemberAccountMapper.class);
        when(accountMapper.selectOne(any(Wrapper.class))).thenReturn(account);
        LoyaltyRedemptionMapper redemptionMapper = mock(LoyaltyRedemptionMapper.class);
        LoyaltyTransactionJournalMapper journalMapper = mock(LoyaltyTransactionJournalMapper.class);
        LoyaltyService service = service(accountMapper, journalMapper, redemptionMapper, mock(LoyaltyRuleMapper.class));

        LoyaltyService.RedemptionView view = service.redeem(7L, "user-1", new LoyaltyService.RedemptionCommand(
                "redeem-2",
                "coupon-100",
                100,
                "coupon redemption"));

        assertThat(view.status()).isEqualTo("FAILED");
        assertThat(view.failureReason()).contains("insufficient");
        ArgumentCaptor<LoyaltyRedemptionDO> redemptionCaptor = ArgumentCaptor.forClass(LoyaltyRedemptionDO.class);
        verify(redemptionMapper).insert(redemptionCaptor.capture());
        assertThat(redemptionCaptor.getValue().getStatus()).isEqualTo("FAILED");
        verify(journalMapper, never()).insert(any(LoyaltyTransactionJournalDO.class));
        verify(accountMapper, never()).updateById(any(LoyaltyMemberAccountDO.class));
    }

    @Test
    void benefitEligibilityUsesCurrentTierOrder() {
        LoyaltyMemberAccountMapper accountMapper = mock(LoyaltyMemberAccountMapper.class);
        when(accountMapper.selectOne(any(Wrapper.class))).thenReturn(account("user-1", 1500, 1500, "GOLD"));
        LoyaltyRuleMapper ruleMapper = mock(LoyaltyRuleMapper.class);
        when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                benefit("birthday_coupon", "BASIC"),
                benefit("gold_shipping", "GOLD"),
                benefit("platinum_service", "PLATINUM")));
        LoyaltyService service = service(accountMapper, mock(LoyaltyTransactionJournalMapper.class),
                mock(LoyaltyRedemptionMapper.class), ruleMapper);

        List<LoyaltyService.BenefitEligibilityView> benefits = service.eligibleBenefits(7L, "user-1");

        assertThat(benefits).extracting(LoyaltyService.BenefitEligibilityView::benefitKey)
                .containsExactly("birthday_coupon", "gold_shipping");
    }

    private LoyaltyService service(LoyaltyMemberAccountMapper accountMapper,
                                   LoyaltyTransactionJournalMapper journalMapper,
                                   LoyaltyRedemptionMapper redemptionMapper,
                                   LoyaltyRuleMapper ruleMapper) {
        return new LoyaltyService(accountMapper, journalMapper, redemptionMapper, ruleMapper, CLOCK);
    }

    private LoyaltyMemberAccountDO account(String userId, int balance, int lifetime, String tier) {
        LoyaltyMemberAccountDO row = new LoyaltyMemberAccountDO();
        row.setId(101L);
        row.setTenantId(7L);
        row.setUserId(userId);
        row.setMemberNo("M-1");
        row.setTierCode(tier);
        row.setPointsBalance(balance);
        row.setLifetimePoints(lifetime);
        row.setStatus("ACTIVE");
        row.setEnrolledAt(LocalDateTime.of(2026, 6, 6, 3, 0));
        return row;
    }

    private LoyaltyRedemptionDO redemptionRow(String key, String status, String failureReason) {
        LoyaltyRedemptionDO row = new LoyaltyRedemptionDO();
        row.setId(201L);
        row.setTenantId(7L);
        row.setUserId("user-1");
        row.setAccountId(101L);
        row.setRedemptionKey(key);
        row.setRewardKey("coupon-20");
        row.setPointsCost(200);
        row.setStatus(status);
        row.setFailureReason(failureReason);
        row.setRedeemedAt(LocalDateTime.of(2026, 6, 6, 3, 0));
        return row;
    }

    private LoyaltyRuleDO benefit(String key, String minTier) {
        LoyaltyRuleDO row = new LoyaltyRuleDO();
        row.setId(301L);
        row.setTenantId(7L);
        row.setRuleKey(key);
        row.setRuleType("BENEFIT");
        row.setBenefitKey(key);
        row.setBenefitName(key);
        row.setMinTierCode(minTier);
        row.setEnabled(1);
        return row;
    }
}
