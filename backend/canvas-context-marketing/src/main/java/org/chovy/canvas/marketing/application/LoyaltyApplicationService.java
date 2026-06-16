package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.marketing.api.LoyaltyFacade;
import org.chovy.canvas.marketing.domain.LoyaltyCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排Loyalty相关的应用层用例。
 */
@Service
public class LoyaltyApplicationService implements LoyaltyFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final LoyaltyCatalog catalog;

    /**
     * 创建LoyaltyApplicationService实例。
     */
    public LoyaltyApplicationService() {
        this(Clock.systemDefaultZone());
    }

    LoyaltyApplicationService(Clock clock) {
        this(new LoyaltyCatalog(clock));
    }

    LoyaltyApplicationService(LoyaltyCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行account业务操作。
     */
    @Override
    public LoyaltyAccountView account(Long tenantId, String userId) {
        return catalog.account(tenantId, userId);
    }

    /**
     * 执行earn业务操作。
     */
    @Override
    public LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command) {
        return catalog.earn(tenantId, userId, command);
    }

    /**
     * 执行redeem业务操作。
     */
    @Override
    public RedemptionView redeem(Long tenantId, String userId, RedemptionCommand command) {
        return catalog.redeem(tenantId, userId, command);
    }

    /**
     * 执行eligibleBenefits业务操作。
     */
    @Override
    public List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        return catalog.eligibleBenefits(tenantId, userId);
    }
}
