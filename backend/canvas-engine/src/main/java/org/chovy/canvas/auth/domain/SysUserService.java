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
    private static final Set<String> ALLOWED_ROLES = Set.of(
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN,
            RoleNames.OPERATOR);

    /** 平台管理员角色只允许平台管理员查看或管理。 */
    private static final Set<String> PLATFORM_ADMIN_ROLES = Set.of(
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN);

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
                        .select("id", "tenant_id", "username", "password", "display_name", "role", "enabled")
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

    /**
     * 查询当前管理员可见的后台用户列表。
     *
     * <p>平台超级管理员可查看全部用户；租户管理员只能查看本租户用户，并过滤平台级管理员角色，
     * 避免租户侧枚举或管理跨租户账号。
     *
     * @param operator 当前操作人上下文
     * @return 按权限裁剪后的用户列表
     */
    public List<SysUserDO> listVisible(TenantContext operator) {
        requireUserAdmin(operator);
        if (operator.isSuperAdmin()) {
            return listAll();
        }
        Long tenantId = requireContextTenantId(operator);
        return sysUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserDO>()
                        .eq("tenant_id", tenantId)
                        .notIn("role", PLATFORM_ADMIN_ROLES));
    }

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("用户名已存在: " + normalizedUsername, e);
        }
        return user;
    }

    /** 更新已有记录，仅修改允许变更的字段。 */
    public void update(Long id, String displayName, String rawPassword, String role, TenantContext operator) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SysUserDO user = sysUserMapper.selectById(id);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        String normalizedRole = role == null ? null : requireRole(role);
        requireManagePermission(operator, user.getTenantId(), user.getRole(), normalizedRole);
        if (displayName != null) user.setDisplayName(displayName);
        if (rawPassword != null && !rawPassword.isBlank()) user.setPassword(encoder.encode(rawPassword));
        if (normalizedRole != null) user.setRole(normalizedRole);
        sysUserMapper.updateById(user);
    }

    /** 禁用记录，使其不再参与后续业务流程。 */
    public void disable(Long id, TenantContext operator) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);
        requireManagePermission(operator, user.getTenantId(), user.getRole(), null);
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
        String normalizedRole = requireText(role, "角色").toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("角色只能是 SUPER_ADMIN、TENANT_ADMIN 或 OPERATOR");
        }
        return normalizedRole;
    }

    /**
     * 校验目标租户 ID 必须存在。
     *
     * @param tenantId 待校验的租户 ID
     * @return 已确认非空的租户 ID
     */
    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户 ID 不能为空");
        }
        return tenantId;
    }

    /**
     * 从操作人上下文中提取租户 ID。
     *
     * @param operator 当前操作人上下文
     * @return 操作人所属租户 ID
     */
    private Long requireContextTenantId(TenantContext operator) {
        if (operator.tenantId() == null) {
            throw new AccessDeniedException("当前用户缺少租户 ID");
        }
        return operator.tenantId();
    }

    /**
     * 校验操作人必须具备用户管理权限。
     *
     * @param operator 当前操作人上下文
     */
    private void requireUserAdmin(TenantContext operator) {
        if (operator == null || (!operator.isSuperAdmin() && !operator.isTenantAdmin())) {
            throw new AccessDeniedException("无权限管理用户");
        }
    }

    /**
     * 校验创建后台用户时的租户和角色边界。
     *
     * @param operator 当前操作人上下文
     * @param targetTenantId 待创建用户所属租户
     * @param targetRole 待创建用户角色
     */
    private void requireCreatePermission(TenantContext operator, Long targetTenantId, String targetRole) {
        // 准备本次处理所需的上下文和中间变量。
        requireUserAdmin(operator);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (operator.isSuperAdmin()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (isPlatformAdminRole(targetRole)) {
            throw new AccessDeniedException("TENANT_ADMIN 不能创建 SUPER_ADMIN 用户");
        }
        Long operatorTenantId = requireContextTenantId(operator);
        if (!operatorTenantId.equals(targetTenantId)) {
            throw new AccessDeniedException("TENANT_ADMIN 只能创建当前租户用户");
        }
    }

    /**
     * 校验更新或禁用用户时的租户和角色边界。
     *
     * @param operator 当前操作人上下文
     * @param targetTenantId 被管理用户所属租户
     * @param currentTargetRole 被管理用户当前角色
     * @param requestedTargetRole 请求更新后的目标角色，可为空
     */
    private void requireManagePermission(
            TenantContext operator,
            Long targetTenantId,
            String currentTargetRole,
            String requestedTargetRole) {
        // 准备本次处理所需的上下文和中间变量。
        requireUserAdmin(operator);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (operator.isSuperAdmin()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (isPlatformAdminRole(currentTargetRole)) {
            throw new AccessDeniedException("TENANT_ADMIN 不能管理 SUPER_ADMIN 用户");
        }
        if (isPlatformAdminRole(requestedTargetRole)) {
            throw new AccessDeniedException("TENANT_ADMIN 不能授予 SUPER_ADMIN 角色");
        }
        Long operatorTenantId = requireContextTenantId(operator);
        if (!operatorTenantId.equals(targetTenantId)) {
            throw new AccessDeniedException("TENANT_ADMIN 只能管理当前租户用户");
        }
    }

    /**
     * 判断角色是否属于平台管理员角色。
     *
     * @param role 待判断的角色
     * @return true 表示该角色属于平台管理范围
     */
    private boolean isPlatformAdminRole(String role) {
        return role != null && PLATFORM_ADMIN_ROLES.contains(role.trim().toUpperCase(Locale.ROOT));
    }
}
