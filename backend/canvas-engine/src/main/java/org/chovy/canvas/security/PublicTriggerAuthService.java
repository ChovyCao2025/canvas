package org.chovy.canvas.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
/**
 * PublicTriggerAuthService 封装安全校验、签名验证或敏感信息处理逻辑。
 */
public class PublicTriggerAuthService {

    private final CanvasHmacVerifier verifier;

    @Autowired
    /**
     * 初始化 PublicTriggerAuthService 实例。
     *
     * @param secret secret 参数，用于 PublicTriggerAuthService 流程中的校验、计算或对象转换。
     */
    public PublicTriggerAuthService(
            @Value("${canvas.public-trigger.secret:${canvas.events.report-secret:}}") String secret) {
        this(secret, Clock.systemUTC());
    }

    /**
     * 初始化 PublicTriggerAuthService 实例。
     *
     * @param secret secret 参数，用于 PublicTriggerAuthService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public PublicTriggerAuthService(String secret, Clock clock) {
        this.verifier = new CanvasHmacVerifier(secret, clock);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param headers 待处理业务值，用于规则计算、转换或外部调用。
     * @param rawBody raw body 参数，用于 verify 流程中的校验、计算或对象转换。
     */
    public void verify(HttpHeaders headers, String rawBody) {
        verifier.verify(headers, rawBody, "公开触发");
    }
}
