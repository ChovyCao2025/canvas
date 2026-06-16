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

    /**
     * timestampheader常量，用于保持控制器内部规则一致。
     */
    public static final String TIMESTAMP_HEADER = CanvasHmacVerifier.TIMESTAMP_HEADER;
    /**
     * signatureheader常量，用于保持控制器内部规则一致。
     */
    public static final String SIGNATURE_HEADER = CanvasHmacVerifier.SIGNATURE_HEADER;

    /**
     * verifier，用于保存请求处理过程中需要的业务数据。
     */
    private final CanvasHmacVerifier verifier;

    /**
     * 创建 EventReportAuthService 实例并注入 web 场景依赖。
     * @param secret secret 参数，用于 EventReportAuthService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public EventReportAuthService(@Value("${canvas.events.report-secret:}") String secret) {
        this(secret, Clock.systemUTC());
    }

    EventReportAuthService(String secret, Clock clock) {
        this.verifier = new CanvasHmacVerifier(secret, clock);
    }

    /**
     * 校验事件上报请求的 HMAC 签名。
     *
     * <p>方法读取时间戳和签名请求头，并使用原始请求体参与验签；校验失败会抛出认证异常，
     * 成功时无返回且不会修改请求体或写入业务数据。</p>
     *
     * @param headers 外部上报请求头，需包含时间戳和签名
     * @param rawBody 未反序列化的原始请求体
     */
    public void verify(HttpHeaders headers, String rawBody) {
        verifier.verify(headers, rawBody, "事件上报");
    }
}
