package org.chovy.canvas.web;

import org.chovy.canvas.security.CanvasHmacVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 事件上报接口的共享密钥签名校验。
 *
 * <p>Spring Security 对 /canvas/events/report 保持匿名放行，控制器在业务处理前用
 * HMAC-SHA256 校验来源系统身份，避免任意外部请求伪造事件触发画布。
 */
@Component
public class EventReportAuthService {

    public static final String TIMESTAMP_HEADER = CanvasHmacVerifier.TIMESTAMP_HEADER;
    public static final String SIGNATURE_HEADER = CanvasHmacVerifier.SIGNATURE_HEADER;

    private final CanvasHmacVerifier verifier;

    @Autowired
    public EventReportAuthService(@Value("${canvas.events.report-secret:}") String secret) {
        this(secret, Clock.systemUTC());
    }

    EventReportAuthService(String secret, Clock clock) {
        this.verifier = new CanvasHmacVerifier(secret, clock);
    }

    public void verify(HttpHeaders headers, String rawBody) {
        verifier.verify(headers, rawBody, "事件上报");
    }
}
