package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.marketing.api.LoyaltyFacade;
import org.chovy.canvas.marketing.domain.LoyaltyCatalog;
import org.springframework.stereotype.Service;

@Service
public class LoyaltyApplicationService implements LoyaltyFacade {

    private final LoyaltyCatalog catalog;

    public LoyaltyApplicationService() {
        this(Clock.systemDefaultZone());
    }

    LoyaltyApplicationService(Clock clock) {
        this(new LoyaltyCatalog(clock));
    }

    LoyaltyApplicationService(LoyaltyCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public LoyaltyAccountView account(Long tenantId, String userId) {
        return catalog.account(tenantId, userId);
    }

    @Override
    public LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command) {
        return catalog.earn(tenantId, userId, command);
    }

    @Override
    public RedemptionView redeem(Long tenantId, String userId, RedemptionCommand command) {
        return catalog.redeem(tenantId, userId, command);
    }

    @Override
    public List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        return catalog.eligibleBenefits(tenantId, userId);
    }
}
