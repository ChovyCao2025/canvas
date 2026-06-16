package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.chovy.canvas.platform.api.AuthFacade;
import org.junit.jupiter.api.Test;

/**
 * 覆盖认证应用服务的登录、锁定、登出和令牌查询行为。
 */
class AuthApplicationServiceTest {

    /**
     * 验证登录返回令牌并清理此前失败次数。
     */
    @Test
    void loginReturnsTokenAndClearsPriorFailures() {
        AuthFacade service = new AuthApplicationService();

        assertThatThrownBy(() -> service.login(new AuthFacade.LoginCommand("admin", "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");

        AuthFacade.LoginView login = service.login(new AuthFacade.LoginCommand("admin", "admin123"));

        assertThat(login)
                .returns("auth-token-1-admin", AuthFacade.LoginView::token)
                .returns(1L, AuthFacade.LoginView::userId)
                .returns(0L, AuthFacade.LoginView::tenantId)
                .returns("admin", AuthFacade.LoginView::username)
                .returns("Administrator", AuthFacade.LoginView::displayName)
                .returns("ADMIN", AuthFacade.LoginView::role);
        assertThat(service.failedAttempts("admin")).isZero();
    }

    /**
     * 验证连续错误密码会锁定账号并拒绝后续登录。
     */
    @Test
    void repeatedBadPasswordLocksAccountAndRejectsLaterLogin() {
        AuthFacade service = new AuthApplicationService();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login(new AuthFacade.LoginCommand("admin", "wrong")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        assertThat(service.isLocked("admin")).isTrue();
        assertThatThrownBy(() -> service.login(new AuthFacade.LoginCommand("admin", "admin123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AUTH_004")
                .hasMessageContaining("账号已锁定");
    }

    /**
     * 验证登出会撤销 Bearer 令牌且当前用户查询拒绝已撤销令牌。
     */
    @Test
    void logoutRevokesBearerTokenAndMeRejectsRevokedToken() {
        AuthFacade service = new AuthApplicationService();
        AuthFacade.LoginView login = service.login(new AuthFacade.LoginCommand("admin", "admin123"));

        assertThat(service.me("Bearer " + login.token()).username()).isEqualTo("admin");

        AuthFacade.LogoutView logout = service.logout("Bearer " + login.token());

        assertThat(logout.revoked()).isTrue();
        assertThat(logout.tokenHash()).hasSize(32);
        assertThatThrownBy(() -> service.me("Bearer " + login.token()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token has been revoked");
    }

    /**
     * 验证格式错误的登出无副作用，但当前用户查询必须提供有效令牌。
     */
    @Test
    void malformedLogoutIsNoOpButMeRequiresKnownBearerToken() {
        AuthFacade service = new AuthApplicationService();

        assertThat(service.logout(null).revoked()).isFalse();
        assertThat(service.logout("Basic abc").revoked()).isFalse();

        assertThatThrownBy(() -> service.me("Basic abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid bearer token is required");
        assertThatThrownBy(() -> service.me("Bearer unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token is not recognized");
    }
}
