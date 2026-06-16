package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.AuthFacade;
import org.chovy.canvas.platform.domain.AuthCatalog;
import org.springframework.stereotype.Service;

/**
 * 认证应用服务，负责把登录和令牌操作委托给认证目录。
 */
@Service
public class AuthApplicationService implements AuthFacade {

    /**
     * 保存用户账号、登录令牌和失败次数的认证目录。
     */
    private final AuthCatalog catalog;

    /**
     * 使用默认内存目录创建认证应用服务。
     */
    public AuthApplicationService() {
        this(new AuthCatalog());
    }

    /**
     * 使用指定认证目录创建应用服务。
     *
     * @param catalog 认证目录
     */
    public AuthApplicationService(AuthCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行用户名密码登录。
     *
     * @param command 登录命令
     * @return 登录结果视图
     */
    @Override
    public LoginView login(LoginCommand command) {
        return catalog.login(command);
    }

    /**
     * 注销授权头对应令牌。
     *
     * @param authorizationHeader HTTP 授权头
     * @return 注销结果视图
     */
    @Override
    public LogoutView logout(String authorizationHeader) {
        return catalog.logout(authorizationHeader);
    }

    /**
     * 查询授权头对应的当前用户。
     *
     * @param authorizationHeader HTTP 授权头
     * @return 当前登录用户视图
     */
    @Override
    public LoginView me(String authorizationHeader) {
        return catalog.me(authorizationHeader);
    }

    /**
     * 注册或更新用户账号。
     *
     * @param command 用户账号命令
     */
    @Override
    public void registerUser(UserCommand command) {
        catalog.registerUser(command);
    }

    /**
     * 查询用户名失败登录次数。
     *
     * @param username 用户名
     * @return 失败登录次数
     */
    @Override
    public int failedAttempts(String username) {
        return catalog.failedAttempts(username);
    }

    /**
     * 判断用户名是否被登录锁定。
     *
     * @param username 用户名
     * @return 已锁定时返回 true
     */
    @Override
    public boolean isLocked(String username) {
        return catalog.isLocked(username);
    }
}
