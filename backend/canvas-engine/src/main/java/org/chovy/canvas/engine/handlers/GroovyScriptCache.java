package org.chovy.canvas.engine.handlers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Groovy 脚本预编译缓存（设计文档 12.9节）。
 *
 * 工作原理：
 *   发布时：CanvasService.publish() 扫描 GROOVY 节点 → precompile() 编译并缓存 Script.class
 *   运行时：getOrCompile() 命中缓存 → 反射实例化（无编译开销）
 *
 * 线程安全：
 *   Script 实例不是线程安全的（持有 Binding 状态），每次执行通过 newInstance() 创建新实例。
 *   缓存的是 Class（编译产物），不是 Script 实例，所以缓存本身线程安全。
 */
@Slf4j
@Component
public class GroovyScriptCache {

    /** key = scriptHash（SHA-256 前16位），value = 编译后的 Script.class */
    private final Cache<String, Class<?>> compiledClasses = Caffeine.newBuilder()
            .maximumSize(500)
            .build();

    /**
     * 预编译（发布时调用，off hot-path）。
     *
     * @param cacheKey  "{canvasId}:{nodeId}:{scriptHash}"
     * @param code      Groovy 脚本内容
     * @param shell     已配置安全沙箱的 GroovyShell（从 GroovyHandler 对象池借取）
     */
    public void precompile(String cacheKey, String code, GroovyShell shell) {
        try {
            Script script = shell.parse(code);
            compiledClasses.put(cacheKey, script.getClass());
            log.debug("[GROOVY_CACHE] 预编译完成 key={}", cacheKey);
        } catch (Exception e) {
            log.warn("[GROOVY_CACHE] 预编译失败 key={}: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * 运行时获取脚本（命中缓存则无编译开销，否则即时编译并缓存）。
     *
     * @return 新的 Script 实例（每次调用返回独立实例，线程安全）
     */
    public Script getOrCompile(String cacheKey, String code, GroovyShell shell) {
        Class<?> cls = compiledClasses.getIfPresent(cacheKey);
        if (cls != null) {
            try {
                return (Script) cls.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.warn("[GROOVY_CACHE] 实例化失败，回退即时编译 key={}", cacheKey);
            }
        }
        // 未命中：即时编译 + 缓存 class
        Script script = shell.parse(code);
        compiledClasses.put(cacheKey, script.getClass());
        return script;
    }

    /** 清除某画布的所有缓存（重新发布时调用） */
    public void evictCanvas(Long canvasId) {
        compiledClasses.asMap().keySet()
                .removeIf(k -> k.startsWith(canvasId + ":"));
    }

    /** 计算脚本 SHA-256（取前 16 字符作为 hash key） */
    public static String hash(String script) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(script.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8); // 16 hex chars
        } catch (Exception e) {
            return String.valueOf(script.hashCode());
        }
    }

    /**
     * 执行 size 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 计算得到的数值结果
     */
    public long size() { return compiledClasses.estimatedSize(); }
}
