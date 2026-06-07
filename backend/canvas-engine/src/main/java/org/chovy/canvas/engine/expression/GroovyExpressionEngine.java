package org.chovy.canvas.engine.expression;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.chovy.canvas.engine.handlers.GroovyScriptCache;
import org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Groovy-backed expression engine adapter.
 */
@Slf4j
@Component
public class GroovyExpressionEngine implements ExpressionEngine {

    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private final GroovyScriptCache scriptCache;
    private final BlockingQueue<GroovyShell> shellPool = new LinkedBlockingQueue<>(POOL_SIZE);
    private final ManagedVirtualThreadExecutor backgroundExecutor;

    @Value("${canvas.groovy.timeout-ms:5000}")
    private long timeoutMs = 5000L;

    @Value("${canvas.groovy.max-output-kb:64}")
    private int maxOutputKb = 64;

    public GroovyExpressionEngine(GroovyScriptCache scriptCache) {
        this(scriptCache, ManagedVirtualThreadExecutor.direct());
    }

    @Autowired
    public GroovyExpressionEngine(GroovyScriptCache scriptCache,
                                  ManagedVirtualThreadExecutor backgroundExecutor) {
        this.scriptCache = scriptCache;
        this.backgroundExecutor = backgroundExecutor == null
                ? ManagedVirtualThreadExecutor.direct()
                : backgroundExecutor;
        CompilerConfiguration cfg = buildConfig();
        for (int i = 0; i < POOL_SIZE; i++) {
            shellPool.offer(new GroovyShell(cfg));
        }
    }

    @Override
    public void precompile(Long canvasId, String nodeId, String code) {
        String cacheKey = cacheKey(canvasId, nodeId, code);
        GroovyShell shell = borrowShell();
        try {
            scriptCache.precompile(cacheKey, code, shell);
        } finally {
            returnShell(shell);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Long canvasId, String nodeId, String code, Map<String, Object> variables)
            throws ExpressionException {
        GroovyShell shell = borrowShell();
        try {
            Binding binding = new Binding();
            variables.forEach(binding::setVariable);

            String cacheKey = cacheKey(canvasId, nodeId, code);
            GroovyShell finalShell = shell;
            Future<Object> future = backgroundExecutor.submit("groovy-expression-" + cacheKey, () -> {
                Script script = scriptCache.getOrCompile(cacheKey, code, finalShell);
                script.setBinding(binding);
                return script.run();
            });

            Object result;
            try {
                result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new ExpressionException("Groovy 脚本执行超时（" + timeoutMs + "ms）", e);
            }

            Map<String, Object> output = result instanceof Map<?, ?> m
                    ? new HashMap<>((Map<String, Object>) m)
                    : Map.of();
            if (output.toString().length() > maxOutputKb * 1024) {
                throw new ExpressionException("Groovy 输出超过大小限制（" + maxOutputKb + "KB）");
            }
            return output;
        } catch (ExpressionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExpressionException("Groovy 执行异常: " + e.getMessage(), e);
        } finally {
            returnShell(shell);
        }
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> variables) throws ExpressionException {
        try {
            Binding binding = new Binding();
            variables.forEach(binding::setVariable);
            GroovyShell shell = new GroovyShell(binding, buildConfig());
            return shell.evaluate(expression);
        } catch (Exception e) {
            throw new ExpressionException("Groovy 表达式执行异常: " + e.getMessage(), e);
        }
    }

    @Override
    public void evictCanvas(Long canvasId) {
        scriptCache.evictCanvas(canvasId);
    }

    private GroovyShell borrowShell() {
        GroovyShell shell = shellPool.poll();
        return shell != null ? shell : new GroovyShell(buildConfig());
    }

    private void returnShell(GroovyShell shell) {
        if (shell != null) {
            shellPool.offer(shell);
        }
    }

    private String cacheKey(Long canvasId, String nodeId, String code) {
        return canvasId + ":" + nodeId + ":" + GroovyScriptCache.hash(code);
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
        security.setDisallowedReceivers(List.of(
                "java.lang.Runtime", "java.lang.Process", "java.lang.ProcessBuilder",
                "java.lang.Thread", "java.lang.ClassLoader", "java.lang.Class",
                "java.lang.reflect.Method", "java.lang.reflect.Field"));
        security.setIndirectImportCheckEnabled(true);

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(security);
        return config;
    }

}
