package org.chovy.canvas.common.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.stereotype.Component;

@Component
public class TenantScopeSupport {

    public <T> LambdaQueryWrapper<T> applyTenantFilter(
            LambdaQueryWrapper<T> wrapper,
            SFunction<T, Long> tenantGetter,
            TenantContext context) {
        if (context.tenantId() == null && RoleNames.ADMIN.equals(context.role())) {
            return wrapper;
        }
        if (context.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return wrapper.eq(tenantGetter, context.tenantId());
    }
}
