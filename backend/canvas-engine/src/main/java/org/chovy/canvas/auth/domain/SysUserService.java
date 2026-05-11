package org.chovy.canvas.auth.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder encoder;

    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    public SysUser findById(Long id) {
        return sysUserMapper.selectById(id);
    }

    public List<SysUser> listAll() {
        return sysUserMapper.selectList(null);
    }

    public SysUser create(String username, String rawPassword, String displayName, String role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(1);
        sysUserMapper.insert(user);
        return user;
    }

    public void update(Long id, String displayName, String rawPassword, String role) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        if (displayName != null) user.setDisplayName(displayName);
        if (rawPassword != null && !rawPassword.isBlank()) user.setPassword(encoder.encode(rawPassword));
        if (role != null) user.setRole(role);
        sysUserMapper.updateById(user);
    }

    public void disable(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        user.setEnabled(0);
        sysUserMapper.updateById(user);
    }

    public boolean checkPassword(SysUser user, String rawPassword) {
        return encoder.matches(rawPassword, user.getPassword());
    }
}
