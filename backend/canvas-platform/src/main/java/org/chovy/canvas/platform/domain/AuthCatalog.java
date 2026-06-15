package org.chovy.canvas.platform.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.chovy.canvas.platform.api.AuthFacade;

public class AuthCatalog {

    private static final int MAX_FAIL_COUNT = 5;

    private final Map<String, UserAccount> usersByUsername = new LinkedHashMap<>();
    private final Map<Long, UserAccount> usersById = new LinkedHashMap<>();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Set<String> lockedUsers = ConcurrentHashMap.newKeySet();
    private final Set<String> revokedTokenHashes = ConcurrentHashMap.newKeySet();

    public AuthCatalog() {
        registerUser(new AuthFacade.UserCommand(1L, 0L, "admin", "admin123", "Administrator", "ADMIN", true));
    }

    public AuthFacade.LoginView login(AuthFacade.LoginCommand command) {
        String username = requireText(command == null ? null : command.username(), "username is required");
        String password = requireText(command.password(), "password is required");
        if (lockedUsers.contains(username)) {
            throw new IllegalArgumentException("AUTH_004: 账号已锁定，请 15 分钟后重试");
        }
        UserAccount user = usersByUsername.get(username);
        if (user == null || !user.enabled() || !user.password().equals(password)) {
            recordFailedAttempt(username);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        failedAttempts.remove(username);
        return toLoginView(user);
    }

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

    public int failedAttempts(String username) {
        return failedAttempts.getOrDefault(username, 0);
    }

    public boolean isLocked(String username) {
        return lockedUsers.contains(username);
    }

    private void recordFailedAttempt(String username) {
        int count = failedAttempts.merge(username, 1, Integer::sum);
        if (count >= MAX_FAIL_COUNT) {
            lockedUsers.add(username);
        }
    }

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

    private static AuthFacade.LoginView toLoginView(UserAccount user) {
        return new AuthFacade.LoginView(token(user), user.userId(), user.tenantId(), user.username(),
                user.displayName(), user.role());
    }

    private static String token(UserAccount user) {
        return "auth-token-" + user.userId() + "-" + user.username();
    }

    private static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private static String tokenHash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (Exception e) {
            return Integer.toHexString(token.hashCode());
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Long requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
    }

    private record UserAccount(
            Long userId,
            Long tenantId,
            String username,
            String password,
            String displayName,
            String role,
            boolean enabled) {
    }
}
