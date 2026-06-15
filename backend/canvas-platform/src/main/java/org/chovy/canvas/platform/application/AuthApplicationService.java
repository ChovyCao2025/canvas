package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.AuthFacade;
import org.chovy.canvas.platform.domain.AuthCatalog;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService implements AuthFacade {

    private final AuthCatalog catalog;

    public AuthApplicationService() {
        this(new AuthCatalog());
    }

    public AuthApplicationService(AuthCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public LoginView login(LoginCommand command) {
        return catalog.login(command);
    }

    @Override
    public LogoutView logout(String authorizationHeader) {
        return catalog.logout(authorizationHeader);
    }

    @Override
    public LoginView me(String authorizationHeader) {
        return catalog.me(authorizationHeader);
    }

    @Override
    public void registerUser(UserCommand command) {
        catalog.registerUser(command);
    }

    @Override
    public int failedAttempts(String username) {
        return catalog.failedAttempts(username);
    }

    @Override
    public boolean isLocked(String username) {
        return catalog.isLocked(username);
    }
}
