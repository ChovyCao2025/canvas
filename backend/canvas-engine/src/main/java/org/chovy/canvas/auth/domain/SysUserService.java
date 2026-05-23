package org.chovy.canvas.auth.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统用户服务（认证与管理员维护）。
 *
 * 边界说明：
 * 1) 仅处理用户读写和密码加密比对；
 * 2) 不负责 JWT 签发与鉴权拦截；
 * 3) 不做 Controller 层参数合法性兜底（由上层先校验）。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    /** 用户表 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** BCrypt 编码器（用于密码加密与比对）。 */
    private final BCryptPasswordEncoder encoder;

    /** 按用户名查询（默认不返回 password 字段）。 */
    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    /** 仅用于登录验证，显式 SELECT password 字段（@TableField(select=false) 默认不查）。 */
    public SysUser findByUsernameForAuth(String username) {
        return sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                        .select("id", "username", "password", "display_name", "role", "enabled")
                        .eq("username", username));
    }

    /** 按 ID 查询用户。 */
    public SysUser findById(Long id) {
        return sysUserMapper.selectById(id);
    }

    /** 查询全部用户（后台管理页使用）。 */
    public List<SysUser> listAll() {
        return sysUserMapper.selectList(null);
    }

    /** 创建用户并对密码进行 BCrypt 加密。 */
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

    /**
     * 更新用户基础信息（仅更新传入字段）。
     *
     * <p>约定：
     * - displayName 为 null 表示“不更新”；
     * - rawPassword 为 null/blank 表示“不更新密码”；
     * - role 为 null 表示“不更新角色”。
     */
    public void update(Long id, String displayName, String rawPassword, String role) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        if (displayName != null) user.setDisplayName(displayName);
        if (rawPassword != null && !rawPassword.isBlank()) user.setPassword(encoder.encode(rawPassword));
        if (role != null) user.setRole(role);
        sysUserMapper.updateById(user);
    }

    /** 禁用用户（enabled=0）。 */
    public void disable(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        user.setEnabled(0);
        sysUserMapper.updateById(user);
    }

    /** 校验明文密码与密文是否匹配。 */
    public boolean checkPassword(SysUser user, String rawPassword) {
        return encoder.matches(rawPassword, user.getPassword());
    }
}
