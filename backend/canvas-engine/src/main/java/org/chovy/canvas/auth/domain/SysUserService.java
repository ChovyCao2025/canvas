package org.chovy.canvas.auth.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "OPERATOR");

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder encoder;

    public SysUserDO findByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserDO>().eq(SysUserDO::getUsername, username));
    }

    /** 仅用于登录验证，显式 SELECT password 字段（@TableField(select=false) 默认不查） */
    public SysUserDO findByUsernameForAuth(String username) {
        return sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserDO>()
                        .select("id", "username", "password", "display_name", "role", "enabled")
                        .eq("username", username));
    }

    public SysUserDO findById(Long id) {
        return sysUserMapper.selectById(id);
    }

    public List<SysUserDO> listAll() {
        return sysUserMapper.selectList(null);
    }

    public SysUserDO create(String username, String rawPassword, String displayName, String role) {
        String normalizedUsername = requireText(username, "用户名");
        String normalizedDisplayName = requireText(displayName, "显示名");
        String normalizedRole = requireRole(role);
        requirePassword(rawPassword);

        if (findByUsername(normalizedUsername) != null) {
            throw new IllegalArgumentException("用户名已存在: " + normalizedUsername);
        }

        SysUserDO user = new SysUserDO();
        user.setUsername(normalizedUsername);
        user.setPassword(encoder.encode(rawPassword));
        user.setDisplayName(normalizedDisplayName);
        user.setRole(normalizedRole);
        user.setEnabled(1);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("用户名已存在: " + normalizedUsername, e);
        }
        return user;
    }

    public void update(Long id, String displayName, String rawPassword, String role) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        if (displayName != null) user.setDisplayName(displayName);
        if (rawPassword != null && !rawPassword.isBlank()) user.setPassword(encoder.encode(rawPassword));
        if (role != null) user.setRole(role);
        sysUserMapper.updateById(user);
    }

    public void disable(Long id) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        user.setEnabled(0);
        sysUserMapper.updateById(user);
    }

    public boolean checkPassword(SysUserDO user, String rawPassword) {
        return encoder.matches(rawPassword, user.getPassword());
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于 6 位");
        }
    }

    private String requireRole(String role) {
        String normalizedRole = requireText(role, "角色");
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("角色只能是 ADMIN 或 OPERATOR");
        }
        return normalizedRole;
    }
}
