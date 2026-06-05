package org.chovy.canvas.auth.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;

/**
 * Sys User 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Test
    void createHashesPasswordAndDoesNotSerializePassword() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(encoder.encode("Secret1")).thenReturn("$2a$encoded-password");
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        SysUserDO created = service.create("operator", "Secret1", "运营人员",
                RoleNames.OPERATOR, 3L, superAdmin());

        ArgumentCaptor<SysUserDO> captor = ArgumentCaptor.forClass(SysUserDO.class);
        verify(sysUserMapper).insert(captor.capture());
        SysUserDO inserted = captor.getValue();
        assertThat(inserted.getUsername()).isEqualTo("operator");
        assertThat(inserted.getPassword()).isEqualTo("$2a$encoded-password");
        assertThat(inserted.getDisplayName()).isEqualTo("运营人员");
        assertThat(inserted.getRole()).isEqualTo(RoleNames.OPERATOR);
        assertThat(inserted.getTenantId()).isEqualTo(3L);
        assertThat(inserted.getEnabled()).isEqualTo(1);

        String json = new ObjectMapper().writeValueAsString(created);
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("encoded-password");
    }

    @Test
    void createRejectsDuplicateUsername() {
        SysUserDO existing = new SysUserDO();
        existing.setUsername("operator");
        when(sysUserMapper.selectOne(any())).thenReturn(existing);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "运营人员",
                RoleNames.OPERATOR, 3L, superAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void createRejectsUnsupportedRole() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "运营人员",
                "GUEST", 3L, superAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("角色");
    }

    @Test
    void createRejectsShortPassword() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "12345", "运营人员",
                RoleNames.OPERATOR, 3L, superAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("密码");
    }

    @Test
    void listVisibleForTenantAdminFiltersCurrentTenantAndPlatformAdmins() {
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        service.listVisible(tenantAdmin(9L));

        ArgumentCaptor<QueryWrapper<SysUserDO>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(sysUserMapper).selectList(captor.capture());
        QueryWrapper<SysUserDO> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("tenant_id").contains("role").contains("NOT IN");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(9L, RoleNames.ADMIN, RoleNames.SUPER_ADMIN);
    }

    @Test
    void tenantAdminCannotCreateSuperAdmin() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("root2", "Secret1", "平台管理员",
                RoleNames.SUPER_ADMIN, 9L, tenantAdmin(9L)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SUPER_ADMIN");
    }

    @Test
    void tenantAdminCannotCreateUserInAnotherTenant() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "运营人员",
                RoleNames.OPERATOR, 10L, tenantAdmin(9L)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("当前租户");
    }

    @Test
    void tenantAdminCanUpdateUserInSameTenant() {
        SysUserDO user = user(88L, 9L, RoleNames.OPERATOR);
        when(sysUserMapper.selectById(88L)).thenReturn(user);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        service.update(88L, "新名称", null, RoleNames.TENANT_ADMIN, tenantAdmin(9L));

        assertThat(user.getDisplayName()).isEqualTo("新名称");
        assertThat(user.getRole()).isEqualTo(RoleNames.TENANT_ADMIN);
        verify(sysUserMapper).updateById(user);
    }

    @Test
    void tenantAdminCannotUpdateUserInAnotherTenant() {
        SysUserDO user = user(88L, 10L, RoleNames.OPERATOR);
        when(sysUserMapper.selectById(88L)).thenReturn(user);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.update(88L, "新名称", null, null, tenantAdmin(9L)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("当前租户");
        verify(sysUserMapper, never()).updateById(any(SysUserDO.class));
    }

    @Test
    void tenantAdminCannotUpdateSameTenantSuperAdmin() {
        SysUserDO user = user(88L, 9L, RoleNames.SUPER_ADMIN);
        when(sysUserMapper.selectById(88L)).thenReturn(user);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.update(88L, "新名称", null, null, tenantAdmin(9L)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SUPER_ADMIN");
        verify(sysUserMapper, never()).updateById(any(SysUserDO.class));
    }

    @Test
    void tenantAdminCannotUpdateSameTenantLegacyAdmin() {
        SysUserDO user = user(88L, 9L, RoleNames.ADMIN);
        when(sysUserMapper.selectById(88L)).thenReturn(user);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.update(88L, "新名称", null, null, tenantAdmin(9L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(sysUserMapper, never()).updateById(any(SysUserDO.class));
    }

    @Test
    void tenantAdminCannotDisableSameTenantSuperAdmin() {
        SysUserDO user = user(88L, 9L, RoleNames.SUPER_ADMIN);
        when(sysUserMapper.selectById(88L)).thenReturn(user);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.disable(88L, tenantAdmin(9L)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SUPER_ADMIN");
        verify(sysUserMapper, never()).updateById(any(SysUserDO.class));
    }

    @Test
    void tenantAdminCannotDisableSameTenantLegacyAdmin() {
        SysUserDO user = user(88L, 9L, RoleNames.ADMIN);
        when(sysUserMapper.selectById(88L)).thenReturn(user);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.disable(88L, tenantAdmin(9L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(sysUserMapper, never()).updateById(any(SysUserDO.class));
    }

    private TenantContext superAdmin() {
        return new TenantContext(1L, RoleNames.SUPER_ADMIN, "root");
    }

    private TenantContext tenantAdmin(Long tenantId) {
        return new TenantContext(tenantId, RoleNames.TENANT_ADMIN, "tenant-admin");
    }

    private SysUserDO user(Long id, Long tenantId, String role) {
        SysUserDO user = new SysUserDO();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setRole(role);
        user.setDisplayName("旧名称");
        user.setEnabled(1);
        return user;
    }
}
