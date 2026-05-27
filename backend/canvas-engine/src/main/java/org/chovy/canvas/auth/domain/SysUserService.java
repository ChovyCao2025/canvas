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

/**
 * 系统用户领域服务。
 *
 * <p>负责后台用户的创建、查询、登录校验和资料更新等认证域能力，并与用户持久化 Mapper 协作。
 * <p>控制器层只负责 HTTP 入参出参转换，用户业务规则在该服务中收敛。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    /** 后台用户允许使用的角色集合。 */
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "OPERATOR");

    /** 系统用户 Mapper，用于认证和后台用户管理。 */
    private final SysUserMapper sysUserMapper;
    /** BCrypt 密码编码器，用于密码加密和校验。 */
    private final BCryptPasswordEncoder encoder;

    /** 按登录用户名查询系统用户，用于管理端展示和唯一性校验。 */
    public SysUserDO findByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserDO>().eq(SysUserDO::getUsername, username));
    }

    /** 仅用于登录验证，显式 SELECT password 字段（@TableField(select=false) 默认不查）。 */
    public SysUserDO findByUsernameForAuth(String username) {
        return sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserDO>()
                        .select("id", "username", "password", "display_name", "role", "enabled")
                        .eq("username", username));
    }

    /** 按主键查询系统用户。 */
    public SysUserDO findById(Long id) {
        return sysUserMapper.selectById(id);
    }

    /** 查询全部后台用户，按创建时间倒序返回。 */
    public List<SysUserDO> listAll() {
        return sysUserMapper.selectList(null);
    }

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
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

    /** 更新已有记录，仅修改允许变更的字段。 */
    public void update(Long id, String displayName, String rawPassword, String role) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        if (displayName != null) user.setDisplayName(displayName);
        if (rawPassword != null && !rawPassword.isBlank()) user.setPassword(encoder.encode(rawPassword));
        if (role != null) user.setRole(role);
        sysUserMapper.updateById(user);
    }

    /** 禁用记录，使其不再参与后续业务流程。 */
    public void disable(Long id) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        user.setEnabled(0);
        sysUserMapper.updateById(user);
    }

    /** 校验明文密码是否匹配用户加密密码。 */
    public boolean checkPassword(SysUserDO user, String rawPassword) {
        return encoder.matches(rawPassword, user.getPassword());
    }

    /** 校验必填文本字段并返回去除首尾空白后的值。 */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    /** 校验创建用户时的明文密码强度下限。 */
    private void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于 6 位");
        }
    }

    /** 校验并规范化后台用户角色，限制为系统允许的角色集合。 */
    private String requireRole(String role) {
        String normalizedRole = requireText(role, "角色");
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("角色只能是 ADMIN 或 OPERATOR");
        }
        return normalizedRole;
    }
}
