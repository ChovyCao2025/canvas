package org.chovy.canvas.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class PublicTriggerAuthService {

    private final CanvasHmacVerifier verifier;

    @Autowired
    public PublicTriggerAuthService(
            @Value("${canvas.public-trigger.secret:${canvas.events.report-secret:}}") String secret) {
        this(secret, Clock.systemUTC());
    }

    public PublicTriggerAuthService(String secret, Clock clock) {
        this.verifier = new CanvasHmacVerifier(secret, clock);
    }

    public void verify(HttpHeaders headers, String rawBody) {
        verifier.verify(headers, rawBody, "公开触发");
    }
}
