package org.chovy.canvas.common.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.stereotype.Component;

/**
 * TenantScopeSupport 提供 common.tenant 场景的通用基础能力。
 */
@Component
public class TenantScopeSupport {

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param wrapper wrapper 参数，用于 applyTenantFilter 流程中的校验、计算或对象转换。
     * @param tenantGetter tenant getter 参数，用于 applyTenantFilter 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 applyTenantFilter 流程生成的业务结果。
     */
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
