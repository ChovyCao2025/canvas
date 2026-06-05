package org.chovy.canvas.common.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantScopeSupportTest {

    private final TenantScopeSupport support = new TenantScopeSupport();

    @Test
    void tenantFilterAddsEqPredicateForTenantUsers() {
        LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<>();

        support.applyTenantFilter(wrapper, CanvasDO::getTenantId,
                new TenantContext(7L, RoleNames.TENANT_ADMIN, "alice"));

        assertThat(wrapper.getExpression().getNormal()).isNotEmpty();
    }

    @Test
    void tenantFilterIsSkippedForLegacyAdminWithoutTenant() {
        LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<>();

        support.applyTenantFilter(wrapper, CanvasDO::getTenantId,
                new TenantContext(null, RoleNames.ADMIN, "root"));

        assertThat(wrapper.getExpression().getNormal()).isEmpty();
    }

    @Test
    void tenantFilterRejectsTenantUserWithoutTenantId() {
        LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<>();

        assertThatThrownBy(() -> support.applyTenantFilter(wrapper, CanvasDO::getTenantId,
                new TenantContext(null, RoleNames.TENANT_ADMIN, "alice")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("AUTH_003: missing tenant context");
    }
}
