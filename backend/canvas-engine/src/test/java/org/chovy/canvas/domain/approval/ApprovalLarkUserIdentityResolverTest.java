package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ApprovalLarkUserIdentityDO;
import org.chovy.canvas.dal.mapper.ApprovalLarkUserIdentityMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalLarkUserIdentityResolverTest {

    @Test
    void resolveReturnsMappedLarkIdentityForTenantUsername() {
        ApprovalLarkUserIdentityMapper mapper = mock(ApprovalLarkUserIdentityMapper.class);
        ApprovalLarkUserIdentityResolver resolver = new ApprovalLarkUserIdentityResolver(mapper);
        ApprovalLarkUserIdentityDO row = new ApprovalLarkUserIdentityDO();
        row.setTenantId(7L);
        row.setUsername("alice");
        row.setLarkOpenId(" ou_alice ");
        row.setLarkUserId("u_alice");
        row.setLarkDepartmentId("od_growth");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row);

        ApprovalLarkUserIdentity identity = resolver.resolve(7L, " alice ");

        assertThat(identity.openId()).isEqualTo("ou_alice");
        assertThat(identity.userId()).isEqualTo("u_alice");
        assertThat(identity.departmentId()).isEqualTo("od_growth");
    }

    @Test
    void resolveReturnsNullWhenNoUsefulMappingExists() {
        ApprovalLarkUserIdentityMapper mapper = mock(ApprovalLarkUserIdentityMapper.class);
        ApprovalLarkUserIdentityResolver resolver = new ApprovalLarkUserIdentityResolver(mapper);
        ApprovalLarkUserIdentityDO blank = new ApprovalLarkUserIdentityDO();
        blank.setLarkOpenId(" ");
        blank.setLarkUserId("");
        blank.setLarkDepartmentId(null);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(blank);

        assertThat(resolver.resolve(7L, "alice")).isNull();

        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThat(resolver.resolve(7L, "alice")).isNull();
        assertThat(resolver.resolve(null, "alice")).isNull();
        assertThat(resolver.resolve(7L, " ")).isNull();
    }
}
