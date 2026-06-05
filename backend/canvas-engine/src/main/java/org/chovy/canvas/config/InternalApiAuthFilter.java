package org.chovy.canvas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiAuthFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Canvas-Internal-Token";

    private final String token;

    public InternalApiAuthFilter(@Value("${canvas.internal-api.token:}") String token) {
        this.token = token == null ? "" : token;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!requiresInternalToken(exchange) || token.isBlank()) {
            return chain.filter(exchange);
        }
        String candidate = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (matches(candidate)) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean requiresInternalToken(ServerWebExchange exchange) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return "/canvas/events/report".equals(path)
                || "/canvas/trigger/behavior".equals(path)
                || isDirectExecutionPath(path);
    }

    private boolean isDirectExecutionPath(String path) {
        String prefix = "/canvas/execute/direct/";
        if (!path.startsWith(prefix)) {
            return false;
        }
        String suffix = path.substring(prefix.length());
        return !suffix.isBlank() && !suffix.contains("/");
    }

    private boolean matches(String candidate) {
        if (candidate == null) {
            return false;
        }
        byte[] expected = token.getBytes(StandardCharsets.UTF_8);
        byte[] actual = candidate.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
