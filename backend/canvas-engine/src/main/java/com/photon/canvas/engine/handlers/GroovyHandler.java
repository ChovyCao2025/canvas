package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;
import groovy.lang.Binding;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

/**
 * Groovy 脚本节点。
 * 安全措施：SecureASTCustomizer 白名单 + 超时 + 输出大小限制 + GroovyShell 对象池。
 */
@Slf4j
@NodeHandlerType("GROOVY")
public class GroovyHandler implements NodeHandler {

    @Value("${canvas.groovy.timeout-ms:5000}")
    private long timeoutMs;

    @Value("${canvas.groovy.max-output-kb:64}")
    private int maxOutputKb;

    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private BlockingQueue<groovy.lang.GroovyShell> shellPool;
    private final ExecutorService vte = Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    void init() {
        CompilerConfiguration cfg = buildConfig();
        shellPool = new LinkedBlockingQueue<>(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            shellPool.offer(new groovy.lang.GroovyShell(cfg));
        }
    }

    private CompilerConfiguration buildConfig() {
        SecureASTCustomizer security = new SecureASTCustomizer();
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
        security.setDisallowedImports(List.of(
                "java.io.*", "java.net.*", "java.lang.Runtime", "java.lang.Process",
                "java.lang.Thread", "java.lang.ClassLoader", "java.lang.reflect.*",
                "sun.*", "com.sun.*"
        ));
        security.setDisallowedMethodNames(List.of("execute", "exec", "exit", "halt",
                "forName", "newInstance", "getDeclaredMethod"));
        security.setIndirectImportCheckEnabled(true);

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(security);
        return config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String code         = (String) config.get("code");
        String nextNodeId   = (String) config.get("nextNodeId");
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) config.get("inputParams");

        if (code == null || code.isBlank()) return NodeResult.ok(nextNodeId, Map.of());

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
            final String finalCode = code;

            Future<Object> future = vte.submit(() -> {
                Script script = finalShell.parse(finalCode);
                script.setBinding(binding);
                return script.run();
            });

            Object result;
            try {
                result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return NodeResult.fail("Groovy 脚本执行超时（" + timeoutMs + "ms）");
            }

            Map<String, Object> output = result instanceof Map<?, ?> m
                    ? new HashMap<>((Map<String, Object>) m) : Map.of();

            // 检查输出大小
            if (output.toString().length() > maxOutputKb * 1024) {
                return NodeResult.fail("Groovy 输出超过大小限制（" + maxOutputKb + "KB）");
            }

            // validateResult 校验
            Boolean validateResult = (Boolean) config.get("validateResult");
            if (Boolean.TRUE.equals(validateResult)) {
                Object res = output.get("result");
                if (!Boolean.TRUE.equals(res) && !"true".equals(String.valueOf(res))) {
                    return NodeResult.fail("Groovy 脚本输出校验不通过");
                }
            }

            return NodeResult.ok(nextNodeId, output);

        } catch (Exception e) {
            log.warn("[GROOVY] 节点执行异常: {}", e.getMessage());
            return NodeResult.fail("Groovy 执行异常: " + e.getMessage());
        } finally {
            if (shell != null) shellPool.offer(shell);
        }
    }
}
