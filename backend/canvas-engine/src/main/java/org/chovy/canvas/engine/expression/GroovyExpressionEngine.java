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
 * 基于 Groovy 的表达式引擎适配器。
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

    /**
     * 使用脚本缓存创建表达式引擎，默认采用直接执行器。
     *
     * @param scriptCache Groovy 脚本缓存
     */
    public GroovyExpressionEngine(GroovyScriptCache scriptCache) {
        this(scriptCache, ManagedVirtualThreadExecutor.direct());
    }

    /**
     * 创建 GroovyExpressionEngine 实例并注入 engine.expression 场景依赖。
     * @param scriptCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param backgroundExecutor 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public GroovyExpressionEngine(GroovyScriptCache scriptCache,
                                  ManagedVirtualThreadExecutor backgroundExecutor) {
        this.scriptCache = scriptCache;
        this.backgroundExecutor = backgroundExecutor == null
                ? ManagedVirtualThreadExecutor.direct()
                : backgroundExecutor;
        CompilerConfiguration cfg = buildConfig();
        // GroovyShell 构造成本较高，预热池化实例可以稳定热路径脚本执行延迟。
        for (int i = 0; i < POOL_SIZE; i++) {
            shellPool.offer(new GroovyShell(cfg));
        }
    }

    /**
     * precompile 处理 engine.expression 场景的业务逻辑。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param code 业务编码，用于匹配对应类型或状态。
     */
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

    /**
     * execute 更新 engine.expression 场景的业务状态。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param code 业务编码，用于匹配对应类型或状态。
     * @param variables variables 参数，用于 execute 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
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
            // 脚本放到独立后台任务中执行，超时取消时可以中断失控的用户代码。
            Future<Object> future = backgroundExecutor.submit("groovy-expression-" + cacheKey, () -> {
                Script script = scriptCache.getOrCompile(cacheKey, code, finalShell);
                script.setBinding(binding);
                return script.run();
            });

            Object result;
            try {
                result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new ExpressionException("Groovy 脚本执行超时（" + timeoutMs + "ms）", e);
            }

            Map<String, Object> output = result instanceof Map<?, ?> m
                    ? new HashMap<>((Map<String, Object>) m)
                    : Map.of();
            // 输出大小在执行后统一限额，避免下游上下文持久化无限膨胀。
            if (output.toString().length() > maxOutputKb * 1024) {
                throw new ExpressionException("Groovy 输出超过大小限制（" + maxOutputKb + "KB）");
            }
            return output;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (ExpressionException e) {
            throw e;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new ExpressionException("Groovy 执行异常: " + e.getMessage(), e);
        } finally {
            returnShell(shell);
        }
    }

    /**
     * evaluate 处理 engine.expression 场景的业务逻辑。
     * @param expression expression 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param variables variables 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    @Override
    public Object evaluate(String expression, Map<String, Object> variables) throws ExpressionException {
        try {
            Binding binding = new Binding();
            variables.forEach(binding::setVariable);
            GroovyShell shell = new GroovyShell(binding, buildConfig());
            return shell.evaluate(expression);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new ExpressionException("Groovy 表达式执行异常: " + e.getMessage(), e);
        }
    }

    /**
     * evictCanvas 删除或清理 engine.expression 场景的业务数据。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     */
    @Override
    public void evictCanvas(Long canvasId) {
        scriptCache.evictCanvas(canvasId);
    }

    /**
     * 从池中借出 GroovyShell，池为空时临时创建。
     *
     * @return 可用于编译或执行脚本的 GroovyShell
     */
    private GroovyShell borrowShell() {
        GroovyShell shell = shellPool.poll();
        return shell != null ? shell : new GroovyShell(buildConfig());
    }

    /**
     * 将 GroovyShell 归还到池中。
     *
     * @param shell 待归还的 GroovyShell，可为空
     */
    private void returnShell(GroovyShell shell) {
        if (shell != null) {
            shellPool.offer(shell);
        }
    }

    /**
     * 生成脚本编译缓存 key。
     *
     * @param canvasId 画布 ID
     * @param nodeId 节点 ID
     * @param code 脚本源码
     * @return 包含脚本哈希的缓存 key
     */
    private String cacheKey(Long canvasId, String nodeId, String code) {
        return canvasId + ":" + nodeId + ":" + GroovyScriptCache.hash(code);
    }

    /**
     * 构建 Groovy 编译安全配置。
     *
     * @return 限制导入和接收者的编译配置
     */
    private CompilerConfiguration buildConfig() {
        SecureASTCustomizer security = new SecureASTCustomizer();
        // 允许列表只开放数据整形常用类型，排除进程、反射和类加载器访问能力。
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
