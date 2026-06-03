package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import reactor.core.publisher.Mono;
import org.chovy.canvas.engine.handler.NodeResult;
import groovy.lang.Binding;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

/**
 * Groovy 脚本节点（含预编译缓存优化，设计文档 12.9节）。
 *
 * 性能优化：
 *   - GroovyShell 对象池（避免重复初始化）
 *   - GroovyScriptCache：发布时预编译 → 运行时直接实例化，无编译开销
 *   - 虚拟线程执行（不占 OS 线程）
 * 安全措施：
 *   - SecureASTCustomizer 白名单
 *   - 超时强制中断
 *   - 输出大小限制 64KB
 */
@Component
@Slf4j
@NodeHandlerType("GROOVY")
@RequiredArgsConstructor
public class GroovyHandler implements NodeHandler {

    /** Groovy 脚本编译缓存，避免运行时重复编译脚本。 */
    private final GroovyScriptCache scriptCache;
    /** 后台任务执行器，用于隔离脚本运行和超时控制。 */
    private final BackgroundTaskExecutor backgroundTaskExecutor;

    /** Groovy 脚本单次执行超时时间，单位毫秒。 */
    @Value("${canvas.groovy.timeout-ms:5000}")
    private long timeoutMs;

    /** Groovy 脚本输出结果最大大小，单位 KB。 */
    @Value("${canvas.groovy.max-output-kb:64}")
    private int maxOutputKb;

    /** GroovyShell 对象池容量。 */
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    /** GroovyShell 对象池，用于复用安全沙箱配置。 */
    private BlockingQueue<groovy.lang.GroovyShell> shellPool;

    /**
     * 注册、调度或初始化 init 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     */
    @PostConstruct
    void init() {
        CompilerConfiguration cfg = buildConfig();
        shellPool = new LinkedBlockingQueue<>(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            shellPool.offer(new groovy.lang.GroovyShell(cfg));
        }
    }

    /** 暴露给 CanvasService.publish() 调用预编译 */
    public void precompileScript(Long canvasId, String nodeId, String code) {
        String cacheKey = canvasId + ":" + nodeId + ":" + GroovyScriptCache.hash(code);
        groovy.lang.GroovyShell shell = shellPool.poll();
        if (shell == null) shell = new groovy.lang.GroovyShell(buildConfig());
        try {
            scriptCache.precompile(cacheKey, code, shell);
        } finally {
            shellPool.offer(shell);
        }
    }

    /** 发布新版本时清除旧编译缓存 */
    public void evictCache(Long canvasId) {
        scriptCache.evictCanvas(canvasId);
    }

    /**
     * 构建、解析或转换 build Config 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 方法执行后的业务结果
     */
    private CompilerConfiguration buildConfig() {
        SecureASTCustomizer security = new SecureASTCustomizer();
        // 仅开放基础类型和常用集合/时间/数学类，脚本不能随意导入系统类。
        security.setAllowedImports(List.of(
                "java.lang.Math", "java.lang.String", "java.lang.StringBuilder",
                "java.lang.Integer", "java.lang.Long", "java.lang.Double",
                "java.lang.Boolean", "java.lang.Number",
                "java.util.List", "java.util.ArrayList", "java.util.Map",
                "java.util.HashMap", "java.util.LinkedHashMap",
                "java.util.Set", "java.util.HashSet",
                "java.util.Collections", "java.util.Arrays", "java.util.Optional",
                "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
                "java.time.Duration", "java.time.format.DateTimeFormatter",
                "java.math.BigDecimal", "java.math.BigInteger", "java.math.RoundingMode",
                "java.util.regex.Pattern", "java.util.regex.Matcher"
        ));
        // setAllowedImports 已是白名单，无需再设黑名单
        security.setDisallowedReceivers(List.of(
                "java.lang.Runtime", "java.lang.Process", "java.lang.ProcessBuilder",
                "java.lang.Thread", "java.lang.ClassLoader", "java.lang.Class",
                "java.lang.reflect.Method", "java.lang.reflect.Field"));
        // 开启间接导入检查，避免脚本通过全限定名绕过导入白名单。
        security.setIndirectImportCheckEnabled(true);

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(security);
        return config;
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String code       = (String) config.get("code");
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) config.get(MapFieldKeys.INPUT_PARAMS);

        if (code == null || code.isBlank()) return Mono.just(NodeResult.ok(nextNodeId, Map.of()));

        // 解析输入参数
        Map<String, Object> input = new HashMap<>();
        if (inputParams != null) {
            for (Map<String, Object> p : inputParams) {
                String name = (String) p.get("name");
                input.put(name, ctx.getContextValue(name));
            }
        }

        groovy.lang.GroovyShell shell = null;
        try {
            shell = shellPool.poll(100, TimeUnit.MILLISECONDS);
            if (shell == null) shell = new groovy.lang.GroovyShell(buildConfig());

            Binding binding = new Binding();
            binding.setVariable("input",       input);
            binding.setVariable("userId",      ctx.getUserId());
            binding.setVariable("canvasId",    String.valueOf(ctx.getCanvasId()));
            binding.setVariable("executionId", ctx.getExecutionId());
            binding.setVariable("ctx",         ctx);

            final groovy.lang.GroovyShell finalShell = shell;

            // 缓存 key（发布时可预编译，运行时命中则无编译开销）
            String cacheKey = ctx.getCanvasId() + ":__groovy__:" + GroovyScriptCache.hash(code);

            // 脚本放到虚拟线程里执行，主响应式链路只等待 Future 结果和超时。
            Future<Object> future = backgroundTaskExecutor.submit("groovy-script-" + ctx.getExecutionId(), () -> {
                Script script = scriptCache.getOrCompile(cacheKey, code, finalShell);
                script.setBinding(binding);
                return script.run();
            });

            Object result;
            try {
                result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return Mono.just(NodeResult.fail("Groovy 脚本执行超时（" + timeoutMs + "ms）"));
            }

            Map<String, Object> output = result instanceof Map<?, ?> m
                    ? new HashMap<>((Map<String, Object>) m) : Map.of();

            if (output.toString().length() > maxOutputKb * 1024) {
                return Mono.just(NodeResult.fail("Groovy 输出超过大小限制（" + maxOutputKb + "KB）"));
            }

            Boolean validateResult = (Boolean) config.get("validateResult");
            if (Boolean.TRUE.equals(validateResult)) {
                List<Map<String, Object>> rules =
                        (List<Map<String, Object>>) config.get(MapFieldKeys.VALIDATE_RULES);
                // 未配置 validateRules 时，约定脚本输出 result=true 才算通过。
                boolean valid = rules == null || rules.isEmpty()
                        ? Boolean.TRUE.equals(output.get("result")) || "true".equals(String.valueOf(output.get("result")))
                        : ConditionEvaluator.allMatch(rules, output);
                if (!valid) {
                    return Mono.just(NodeResult.fail("Groovy 脚本输出校验不通过"));
                }
            }

            return Mono.just(NodeResult.ok(nextNodeId, output));

        } catch (Exception e) {
            log.warn("[GROOVY] 节点执行异常: {}", e.getMessage());
            return Mono.just(NodeResult.fail("Groovy 执行异常: " + e.getMessage()));
        } finally {
            if (shell != null) shellPool.offer(shell);
        }
    }

    /**
     * 轻量表达式求值，供其他 Handler（如 AggregateHandler）复用安全沙箱。
     * 调用方自行构建 Binding，表达式应返回 Boolean。
     */
    public Object evaluateExpression(String expression, Binding binding) throws Exception {
        groovy.lang.GroovyShell shell = new groovy.lang.GroovyShell(binding, buildConfig());
        return shell.evaluate(expression);
    }
}
