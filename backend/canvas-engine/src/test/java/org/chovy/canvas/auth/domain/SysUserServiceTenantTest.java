package org.chovy.canvas.auth.domain;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.jsonwebtoken.Claims;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.auth.util.JwtUtil;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceTenantTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private BCryptPasswordEncoder encoder;

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SysUserDO.class);
    }

    @Test
    void jwtIncludesTenantAndRoleClaims() {
        SysUserDO user = new SysUserDO();
        user.setId(7L);
        user.setTenantId(3L);
        user.setUsername("tenant_admin");
        user.setDisplayName("Tenant Admin");
        user.setRole(RoleNames.TENANT_ADMIN);
        JwtUtil jwtUtil = new JwtUtil("canvas-engine-jwt-secret-key-must-be-at-least-256-bits", 24);

        Claims claims = jwtUtil.parse(jwtUtil.generate(user));

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("tenantId", Number.class).longValue()).isEqualTo(3L);
        assertThat(claims.get("role", String.class)).isEqualTo(RoleNames.TENANT_ADMIN);
    }

    @Test
    void legacyAdminRoleIsSuperAdmin() {
        TenantContext context = new TenantContext(3L, RoleNames.ADMIN, "admin");

        assertThat(context.isSuperAdmin()).isTrue();
    }

    @Test
    void createRejectsMissingTenantId() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "Operator",
                RoleNames.OPERATOR, null, superAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("租户");
    }

    @Test
    void tenantAdminCannotCreateUserInAnotherTenant() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "Operator",
                RoleNames.OPERATOR, 9L, tenantAdmin()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("当前租户");
    }

    @Test
    void tenantAdminCannotCreateSuperAdmin() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "Operator",
                RoleNames.SUPER_ADMIN, 3L, tenantAdmin()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SUPER_ADMIN");
    }

    @Test
    void superAdminAndLegacyAdminCanCreateUsersInAnotherTenant() {
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(encoder.encode("Secret1")).thenReturn("$2a$encoded-password");
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        service.create("operator1", "Secret1", "Operator 1",
                RoleNames.OPERATOR, 9L, superAdmin());
        service.create("operator2", "Secret1", "Operator 2",
                RoleNames.OPERATOR, 10L, legacyAdmin());

        ArgumentCaptor<SysUserDO> captor = ArgumentCaptor.forClass(SysUserDO.class);
        verify(sysUserMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SysUserDO::getTenantId)
                .containsExactly(9L, 10L);
    }

    @Test
    void tenantAdminListsOnlyUsersInOwnTenant() {
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        service.listVisible(tenantAdmin());

        ArgumentCaptor<Wrapper<SysUserDO>> captor = wrapperCaptor();
        verify(sysUserMapper).selectList(captor.capture());
        Wrapper<SysUserDO> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("tenant_id");
        assertThat(((LambdaQueryWrapper<SysUserDO>) wrapper).getParamNameValuePairs())
                .containsValue(3L);
    }

    @Test
    void superAdminListsAllUsers() {
        when(sysUserMapper.selectList(null)).thenReturn(List.of());
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        service.listVisible(superAdmin());

        verify(sysUserMapper).selectList(null);
    }

    private TenantContext superAdmin() {
        return new TenantContext(1L, RoleNames.SUPER_ADMIN, "root");
    }

    private TenantContext legacyAdmin() {
        return new TenantContext(1L, RoleNames.ADMIN, "legacy_admin");
    }

    private TenantContext tenantAdmin() {
        return new TenantContext(3L, RoleNames.TENANT_ADMIN, "tenant_admin");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
