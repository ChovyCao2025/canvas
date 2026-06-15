package org.chovy.canvas.platform.api;

public interface AuthFacade {

    LoginView login(LoginCommand command);

    LogoutView logout(String authorizationHeader);

    LoginView me(String authorizationHeader);

    default void registerUser(UserCommand command) {
    }

    default int failedAttempts(String username) {
        return 0;
    }

    default boolean isLocked(String username) {
        return false;
    }

    record LoginCommand(String username, String password) {
    }

    record UserCommand(
            Long userId,
            Long tenantId,
            String username,
            String password,
            String displayName,
            String role,
            boolean enabled) {
    }

    record LoginView(String token, Long userId, Long tenantId, String username, String displayName, String role) {
    }

    record LogoutView(boolean revoked, String tokenHash) {
    }
}
