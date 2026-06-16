package org.chovy.canvas.platform.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.chovy.canvas.platform.api.AuthFacade;

/**
 * 认证目录，保存演示用户、失败次数、锁定状态和已撤销令牌。
 */
public class AuthCatalog {

    /**
     * 用户被锁定前允许的最大失败次数。
     */
    private static final int MAX_FAIL_COUNT = 5;

    /**
     * 按用户名索引的用户账号。
     */
    private final Map<String, UserAccount> usersByUsername = new LinkedHashMap<>();

    /**
     * 按用户标识索引的用户账号。
     */
    private final Map<Long, UserAccount> usersById = new LinkedHashMap<>();

    /**
     * 用户名对应的失败登录次数。
     */
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    /**
     * 已锁定用户名集合。
     */
    private final Set<String> lockedUsers = ConcurrentHashMap.newKeySet();

    /**
     * 已撤销令牌哈希集合。
     */
    private final Set<String> revokedTokenHashes = ConcurrentHashMap.newKeySet();

    /**
     * 创建认证目录并注册默认管理员。
     */
    public AuthCatalog() {
        registerUser(new AuthFacade.UserCommand(1L, 0L, "admin", "admin123", "Administrator", "ADMIN", true));
    }

    /**
     * 使用用户名和密码登录。
     *
     * @param command 登录命令
     * @return 登录结果视图
     */
    public AuthFacade.LoginView login(AuthFacade.LoginCommand command) {
        String username = requireText(command == null ? null : command.username(), "username is required");
        String password = requireText(command.password(), "password is required");
        if (lockedUsers.contains(username)) {
            throw new IllegalArgumentException("AUTH_004: 账号已锁定，请 15 分钟后重试");
        }
        UserAccount user = usersByUsername.get(username);
        if (user == null || !user.enabled() || !user.password().equals(password)) {
            // 只对用户名维度记录失败次数，保持锁定逻辑与登录表单字段一致。
            recordFailedAttempt(username);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        failedAttempts.remove(username);
        return toLoginView(user);
    }

    /**
     * 注销授权头中的令牌。
     *
     * @param authorizationHeader HTTP 授权头
     * @return 注销结果视图
     */
    public AuthFacade.LogoutView logout(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        if (token == null || token.isBlank()) {
            return new AuthFacade.LogoutView(false, null);
        }
        UserAccount user = userFromToken(token);
        if (user == null) {
            return new AuthFacade.LogoutView(false, null);
        }
        String hash = tokenHash(token);
        revokedTokenHashes.add(hash);
        return new AuthFacade.LogoutView(true, hash);
    }

    /**
     * 查询授权头对应的当前用户。
     *
     * @param authorizationHeader HTTP 授权头
     * @return 当前登录用户视图
     */
    public AuthFacade.LoginView me(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("valid bearer token is required");
        }
        if (revokedTokenHashes.contains(tokenHash(token))) {
            throw new IllegalArgumentException("token has been revoked");
        }
        UserAccount user = userFromToken(token);
        if (user == null) {
            throw new IllegalArgumentException("token is not recognized");
        }
        return toLoginView(user);
    }

    /**
     * 注册或更新用户账号。
     *
     * @param command 用户账号命令
     */
    public void registerUser(AuthFacade.UserCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("user command is required");
        }
        Long userId = requireId(command.userId(), "userId is required");
        Long tenantId = command.tenantId() == null ? 0L : command.tenantId();
        String username = requireText(command.username(), "username is required");
        String password = requireText(command.password(), "password is required");
        String displayName = command.displayName() == null || command.displayName().isBlank()
                ? username
                : command.displayName().trim();
        String role = command.role() == null || command.role().isBlank() ? "USER" : command.role().trim();
        UserAccount user = new UserAccount(userId, tenantId, username, password, displayName, role, command.enabled());
        usersByUsername.put(username, user);
        usersById.put(userId, user);
    }

    /**
     * 查询用户名失败登录次数。
     *
     * @param username 用户名
     * @return 失败登录次数
     */
    public int failedAttempts(String username) {
        return failedAttempts.getOrDefault(username, 0);
    }

    /**
     * 判断用户名是否已锁定。
     *
     * @param username 用户名
     * @return 已锁定时返回 true
     */
    public boolean isLocked(String username) {
        return lockedUsers.contains(username);
    }

    /**
     * 记录一次失败登录并在达到阈值时锁定用户。
     *
     * @param username 用户名
     */
    private void recordFailedAttempt(String username) {
        int count = failedAttempts.merge(username, 1, Integer::sum);
        if (count >= MAX_FAIL_COUNT) {
            lockedUsers.add(username);
        }
    }

    /**
     * 从演示令牌中解析用户账号。
     *
     * @param token 演示令牌
     * @return 令牌对应用户；无效令牌返回 null
     */
    private UserAccount userFromToken(String token) {
        String[] parts = token.split("-", 4);
        if (parts.length != 4 || !"auth".equals(parts[0]) || !"token".equals(parts[1])) {
            return null;
        }
        try {
            Long userId = Long.parseLong(parts[2]);
            UserAccount user = usersById.get(userId);
            if (user == null || !user.username().equals(parts[3])) {
                return null;
            }
            return user;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 将用户账号转换为登录视图。
     *
     * @param user 用户账号
     * @return 登录视图
     */
    private static AuthFacade.LoginView toLoginView(UserAccount user) {
        return new AuthFacade.LoginView(token(user), user.userId(), user.tenantId(), user.username(),
                user.displayName(), user.role());
    }

    /**
     * 生成演示登录令牌。
     *
     * @param user 用户账号
     * @return 演示登录令牌
     */
    private static String token(UserAccount user) {
        return "auth-token-" + user.userId() + "-" + user.username();
    }

    /**
     * 从授权头解析 Bearer 令牌。
     *
     * @param authorizationHeader HTTP 授权头
     * @return Bearer 令牌；格式不匹配时返回 null
     */
    private static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    /**
     * 计算令牌哈希。
     *
     * @param token 原始令牌
     * @return 令牌哈希
     */
    private static String tokenHash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (Exception e) {
            // 极端情况下 SHA-256 不可用时退化到稳定哈希，保证登出流程仍能记录撤销标识。
            return Integer.toHexString(token.hashCode());
        }
    }

    /**
     * 校验并修剪必填文本。
     *
     * @param value 原始文本
     * @param message 校验失败时使用的异常消息
     * @return 修剪后的文本
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 校验正数标识。
     *
     * @param id 原始标识
     * @param message 校验失败时使用的异常消息
     * @return 合法标识
     */
    private static Long requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
    }

    /**
     * 认证目录内部用户账号。
     *
     */
    private static final class UserAccount {

        /**
         * 用户标识。
         */
        private final Long userId;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 用户名。
         */
        private final String username;

        /**
         * 明文密码。
         */
        private final String password;

        /**
         * 展示名称。
         */
        private final String displayName;

        /**
         * 用户角色。
         */
        private final String role;

        /**
         * 是否启用。
         */
        private final boolean enabled;

        /**
         * 创建认证目录内部用户账号。
         *
         * @param userId 用户标识
         * @param tenantId 租户标识
         * @param username 用户名
         * @param password 明文密码
         * @param displayName 展示名称
         * @param role 用户角色
         * @param enabled 是否启用
         */
        private UserAccount(Long userId, Long tenantId, String username, String password, String displayName,
                String role, boolean enabled) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.username = username;
            this.password = password;
            this.displayName = displayName;
            this.role = role;
            this.enabled = enabled;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        private Long userId() {
            return userId;
        }

        /**
         * 返回租户标识。
         *
         * @return 租户标识
         */
        private Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户名。
         *
         * @return 用户名
         */
        private String username() {
            return username;
        }

        /**
         * 返回明文密码。
         *
         * @return 明文密码
         */
        private String password() {
            return password;
        }

        /**
         * 返回展示名称。
         *
         * @return 展示名称
         */
        private String displayName() {
            return displayName;
        }

        /**
         * 返回用户角色。
         *
         * @return 用户角色
         */
        private String role() {
            return role;
        }

        /**
         * 返回是否启用。
         *
         * @return 是否启用
         */
        private boolean enabled() {
            return enabled;
        }

        /**
         * 判断两个内部用户账号是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof UserAccount that)) {
                return false;
            }
            return enabled == that.enabled
                    && Objects.equals(userId, that.userId)
                    && Objects.equals(tenantId, that.tenantId)
                    && Objects.equals(username, that.username)
                    && Objects.equals(password, that.password)
                    && Objects.equals(displayName, that.displayName)
                    && Objects.equals(role, that.role);
        }

        /**
         * 计算内部用户账号哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(userId, tenantId, username, password, displayName, role, enabled);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "UserAccount[userId=" + userId + ", tenantId=" + tenantId + ", username=" + username
                    + ", password=" + password + ", displayName=" + displayName + ", role=" + role
                    + ", enabled=" + enabled + "]";
        }
    }
}
