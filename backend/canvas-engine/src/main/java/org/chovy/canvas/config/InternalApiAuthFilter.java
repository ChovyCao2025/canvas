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

/**
 * InternalApiAuthFilter 提供 config 场景的 Spring 配置或启动校验。
 */
@Component
public class InternalApiAuthFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Canvas-Internal-Token";

    private final String token;

    /**
     * 创建 InternalApiAuthFilter 实例并注入 config 场景依赖。
     * @param token 令牌或锁标识，用于鉴权、幂等或并发控制。
     */
    public InternalApiAuthFilter(@Value("${canvas.internal-api.token:}") String token) {
        this.token = token == null ? "" : token;
    }

    /**
     * filter 处理 config 场景的业务逻辑。
     * @param exchange exchange 参数，用于 filter 流程中的校验、计算或对象转换。
     * @param chain chain 参数，用于 filter 流程中的校验、计算或对象转换。
     * @return 返回 filter 流程生成的业务结果。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!requiresInternalToken(exchange) || token.isBlank()) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return chain.filter(exchange);
        }
        String candidate = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (matches(candidate)) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return exchange.getResponse().setComplete();
    }

    /**
     * 判断当前请求是否属于需要内部令牌保护的匿名入口。
     *
     * @param exchange 当前请求交换对象
     * @return true 表示需要校验内部令牌
     */
    private boolean requiresInternalToken(ServerWebExchange exchange) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return "/canvas/events/report".equals(path)
                || "/canvas/trigger/behavior".equals(path)
                || "/warehouse/realtime/pipelines/checkpoints".equals(path)
                /**
                 * 判断业务条件是否成立。
                 *
                 * @param path path 参数，用于 isDirectExecutionPath 流程中的校验、计算或对象转换。
                 * @return 返回布尔判断结果。
                 */
                || isDirectExecutionPath(path);
    }

    /**
     * 判断路径是否为单画布直调执行入口。
     *
     * @param path 请求路径
     * @return true 表示匹配直调执行路径
     */
    private boolean isDirectExecutionPath(String path) {
        String prefix = "/canvas/execute/direct/";
        if (!path.startsWith(prefix)) {
            return false;
        }
        String suffix = path.substring(prefix.length());
        return !suffix.isBlank() && !suffix.contains("/");
    }

    /**
     * 使用常量时间比较校验候选令牌。
     *
     * @param candidate 请求头中的令牌
     * @return true 表示令牌匹配
     */
    private boolean matches(String candidate) {
        if (candidate == null) {
            return false;
        }
        byte[] expected = token.getBytes(StandardCharsets.UTF_8);
        byte[] actual = candidate.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
