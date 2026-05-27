package org.chovy.canvas.auth.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private TenantContext superAdmin() {
        return new TenantContext(1L, RoleNames.SUPER_ADMIN, "root");
    }
}
