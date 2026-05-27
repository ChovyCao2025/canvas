package org.chovy.canvas.auth.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN,
            RoleNames.OPERATOR);

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
                        .select("id", "tenant_id", "username", "password", "display_name", "role", "enabled")
                        .eq("username", username));
    }

    public SysUserDO findById(Long id) {
        return sysUserMapper.selectById(id);
    }

    public List<SysUserDO> listAll() {
        return sysUserMapper.selectList(null);
    }

    public List<SysUserDO> listVisible(TenantContext operator) {
        requireUserAdmin(operator);
        if (operator.isSuperAdmin()) {
            return listAll();
        }
        Long tenantId = requireContextTenantId(operator);
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUserDO>().eq(SysUserDO::getTenantId, tenantId));
    }

    public SysUserDO create(String username, String rawPassword, String displayName,
                            String role, Long tenantId, TenantContext operator) {
        String normalizedUsername = requireText(username, "用户名");
        String normalizedDisplayName = requireText(displayName, "显示名");
        String normalizedRole = requireRole(role);
        Long targetTenantId = requireTenantId(tenantId);
        requireCreatePermission(operator, targetTenantId, normalizedRole);
        requirePassword(rawPassword);

        if (findByUsername(normalizedUsername) != null) {
            throw new IllegalArgumentException("用户名已存在: " + normalizedUsername);
        }

        SysUserDO user = new SysUserDO();
        user.setTenantId(targetTenantId);
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

    public void update(Long id, String displayName, String rawPassword, String role, TenantContext operator) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        requireManagePermission(operator, user.getTenantId(), role);
        if (displayName != null) user.setDisplayName(displayName);
        if (rawPassword != null && !rawPassword.isBlank()) user.setPassword(encoder.encode(rawPassword));
        if (role != null) user.setRole(requireRole(role));
        sysUserMapper.updateById(user);
    }

    public void disable(Long id, TenantContext operator) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        requireManagePermission(operator, user.getTenantId(), null);
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
        String normalizedRole = requireText(role, "角色").toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("角色只能是 SUPER_ADMIN、TENANT_ADMIN 或 OPERATOR");
        }
        return normalizedRole;
    }

    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户 ID 不能为空");
        }
        return tenantId;
    }

    private Long requireContextTenantId(TenantContext operator) {
        if (operator.tenantId() == null) {
            throw new AccessDeniedException("当前用户缺少租户 ID");
        }
        return operator.tenantId();
    }

    private void requireUserAdmin(TenantContext operator) {
        if (operator == null || (!operator.isSuperAdmin() && !operator.isTenantAdmin())) {
            throw new AccessDeniedException("无权限管理用户");
        }
    }

    private void requireCreatePermission(TenantContext operator, Long targetTenantId, String targetRole) {
        requireUserAdmin(operator);
        if (operator.isSuperAdmin()) {
            return;
        }
        if (RoleNames.SUPER_ADMIN.equals(targetRole)) {
            throw new AccessDeniedException("TENANT_ADMIN 不能创建 SUPER_ADMIN 用户");
        }
        Long operatorTenantId = requireContextTenantId(operator);
        if (!operatorTenantId.equals(targetTenantId)) {
            throw new AccessDeniedException("TENANT_ADMIN 只能创建当前租户用户");
        }
    }

    private void requireManagePermission(TenantContext operator, Long targetTenantId, String targetRole) {
        requireUserAdmin(operator);
        if (operator.isSuperAdmin()) {
            return;
        }
        if (targetRole != null && RoleNames.SUPER_ADMIN.equals(requireRole(targetRole))) {
            throw new AccessDeniedException("TENANT_ADMIN 不能授予 SUPER_ADMIN 角色");
        }
        Long operatorTenantId = requireContextTenantId(operator);
        if (!operatorTenantId.equals(targetTenantId)) {
            throw new AccessDeniedException("TENANT_ADMIN 只能管理当前租户用户");
        }
    }
}
