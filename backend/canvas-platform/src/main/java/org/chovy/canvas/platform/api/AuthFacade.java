package org.chovy.canvas.platform.api;

import java.util.Objects;

/**
 * 提供登录、登出、当前用户查询和测试账号管理能力的应用入口。
 */
public interface AuthFacade {

    /**
     * 使用用户名和密码登录。
     *
     * @param command 登录命令
     * @return 登录结果视图
     */
    LoginView login(LoginCommand command);

    /**
     * 注销授权头对应的登录令牌。
     *
     * @param authorizationHeader HTTP 授权头
     * @return 注销结果视图
     */
    LogoutView logout(String authorizationHeader);

    /**
     * 查询授权头对应的当前登录用户。
     *
     * @param authorizationHeader HTTP 授权头
     * @return 当前登录用户视图
     */
    LoginView me(String authorizationHeader);

    /**
     * 注册或更新测试用户账号。
     *
     * @param command 用户账号命令
     */
    default void registerUser(UserCommand command) {
    }

    /**
     * 查询用户名当前失败登录次数。
     *
     * @param username 用户名
     * @return 失败登录次数
     */
    default int failedAttempts(String username) {
        return 0;
    }

    /**
     * 判断用户名是否已被锁定。
     *
     * @param username 用户名
     * @return 已锁定时返回 true
     */
    default boolean isLocked(String username) {
        return false;
    }

    /**
     * 登录命令。
     *
     */
    final class LoginCommand {

        /**
         * 用户名。
         */
        private final String username;

        /**
         * 明文密码。
         */
        private final String password;

        /**
         * 创建登录命令。
         *
         * @param username 用户名
         * @param password 明文密码
         */
        public LoginCommand(String username, String password) {
            this.username = username;
            this.password = password;
        }

        /**
         * 返回用户名。
         *
         * @return 用户名
         */
        public String username() {
            return username;
        }

        /**
         * 返回明文密码。
         *
         * @return 明文密码
         */
        public String password() {
            return password;
        }

        /**
         * 判断两个登录命令是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof LoginCommand that)) {
                return false;
            }
            return Objects.equals(username, that.username) && Objects.equals(password, that.password);
        }

        /**
         * 计算登录命令哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(username, password);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "LoginCommand[username=" + username + ", password=" + password + "]";
        }
    }

    /**
     * 用户账号注册或更新命令。
     */
    final class UserCommand {

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
         * 创建用户账号注册或更新命令。
         *
         * @param userId 用户标识
         * @param tenantId 租户标识
         * @param username 用户名
         * @param password 明文密码
         * @param displayName 展示名称
         * @param role 用户角色
         * @param enabled 是否启用
         */
        public UserCommand(Long userId, Long tenantId, String username, String password, String displayName,
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
        public Long userId() {
            return userId;
        }

        /**
         * 返回租户标识。
         *
         * @return 租户标识
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户名。
         *
         * @return 用户名
         */
        public String username() {
            return username;
        }

        /**
         * 返回明文密码。
         *
         * @return 明文密码
         */
        public String password() {
            return password;
        }

        /**
         * 返回展示名称。
         *
         * @return 展示名称
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回用户角色。
         *
         * @return 用户角色
         */
        public String role() {
            return role;
        }

        /**
         * 返回是否启用。
         *
         * @return 是否启用
         */
        public boolean enabled() {
            return enabled;
        }

        /**
         * 判断两个用户账号命令是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof UserCommand that)) {
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
         * 计算用户账号命令哈希值。
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
            return "UserCommand[userId=" + userId + ", tenantId=" + tenantId + ", username=" + username
                    + ", password=" + password + ", displayName=" + displayName + ", role=" + role
                    + ", enabled=" + enabled + "]";
        }
    }

    /**
     * 登录成功或当前用户查询视图。
     */
    final class LoginView {

        /**
         * 登录令牌。
         */
        private final String token;

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
         * 展示名称。
         */
        private final String displayName;

        /**
         * 用户角色。
         */
        private final String role;

        /**
         * 创建登录成功或当前用户查询视图。
         *
         * @param token 登录令牌
         * @param userId 用户标识
         * @param tenantId 租户标识
         * @param username 用户名
         * @param displayName 展示名称
         * @param role 用户角色
         */
        public LoginView(String token, Long userId, Long tenantId, String username, String displayName, String role) {
            this.token = token;
            this.userId = userId;
            this.tenantId = tenantId;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
        }

        /**
         * 返回登录令牌。
         *
         * @return 登录令牌
         */
        public String token() {
            return token;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public Long userId() {
            return userId;
        }

        /**
         * 返回租户标识。
         *
         * @return 租户标识
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户名。
         *
         * @return 用户名
         */
        public String username() {
            return username;
        }

        /**
         * 返回展示名称。
         *
         * @return 展示名称
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回用户角色。
         *
         * @return 用户角色
         */
        public String role() {
            return role;
        }

        /**
         * 判断两个登录视图是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof LoginView that)) {
                return false;
            }
            return Objects.equals(token, that.token)
                    && Objects.equals(userId, that.userId)
                    && Objects.equals(tenantId, that.tenantId)
                    && Objects.equals(username, that.username)
                    && Objects.equals(displayName, that.displayName)
                    && Objects.equals(role, that.role);
        }

        /**
         * 计算登录视图哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(token, userId, tenantId, username, displayName, role);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "LoginView[token=" + token + ", userId=" + userId + ", tenantId=" + tenantId
                    + ", username=" + username + ", displayName=" + displayName + ", role=" + role + "]";
        }
    }

    /**
     * 登出结果视图。
     */
    final class LogoutView {

        /**
         * 是否已撤销令牌。
         */
        private final boolean revoked;

        /**
         * 被撤销令牌的哈希值。
         */
        private final String tokenHash;

        /**
         * 创建登出结果视图。
         *
         * @param revoked 是否已撤销令牌
         * @param tokenHash 被撤销令牌的哈希值
         */
        public LogoutView(boolean revoked, String tokenHash) {
            this.revoked = revoked;
            this.tokenHash = tokenHash;
        }

        /**
         * 返回是否已撤销令牌。
         *
         * @return 是否已撤销令牌
         */
        public boolean revoked() {
            return revoked;
        }

        /**
         * 返回被撤销令牌的哈希值。
         *
         * @return 被撤销令牌的哈希值
         */
        public String tokenHash() {
            return tokenHash;
        }

        /**
         * 判断两个登出视图是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof LogoutView that)) {
                return false;
            }
            return revoked == that.revoked && Objects.equals(tokenHash, that.tokenHash);
        }

        /**
         * 计算登出视图哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(revoked, tokenHash);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "LogoutView[revoked=" + revoked + ", tokenHash=" + tokenHash + "]";
        }
    }
}
