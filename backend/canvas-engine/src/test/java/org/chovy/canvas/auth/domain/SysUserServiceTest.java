package org.chovy.canvas.auth.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
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

        SysUser created = service.create("operator", "Secret1", "运营人员", "OPERATOR");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        SysUser inserted = captor.getValue();
        assertThat(inserted.getUsername()).isEqualTo("operator");
        assertThat(inserted.getPassword()).isEqualTo("$2a$encoded-password");
        assertThat(inserted.getDisplayName()).isEqualTo("运营人员");
        assertThat(inserted.getRole()).isEqualTo("OPERATOR");
        assertThat(inserted.getEnabled()).isEqualTo(1);

        String json = new ObjectMapper().writeValueAsString(created);
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("encoded-password");
    }

    @Test
    void createRejectsDuplicateUsername() {
        SysUser existing = new SysUser();
        existing.setUsername("operator");
        when(sysUserMapper.selectOne(any())).thenReturn(existing);
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "运营人员", "OPERATOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void createRejectsUnsupportedRole() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "Secret1", "运营人员", "GUEST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("角色");
    }

    @Test
    void createRejectsShortPassword() {
        SysUserService service = new SysUserService(sysUserMapper, encoder);

        assertThatThrownBy(() -> service.create("operator", "12345", "运营人员", "OPERATOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("密码");
    }
}
