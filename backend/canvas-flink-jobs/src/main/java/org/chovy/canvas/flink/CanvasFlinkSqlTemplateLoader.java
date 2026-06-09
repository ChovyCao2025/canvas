package org.chovy.canvas.flink;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CanvasFlinkSqlTemplateLoader 支撑 flink 场景的后端处理。
 */
public final class CanvasFlinkSqlTemplateLoader {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z0-9_]+)}");

    /**
     * SQL 模板加载器为静态工具类，不允许实例化。
     */
    private CanvasFlinkSqlTemplateLoader() {
    }

    /**
     * 从 classpath 加载 Flink SQL 模板文件。
     *
     * <p>模板缺失或读取失败会抛出异常，让作业在提交前失败，避免启动一个没有 SQL 定义的空作业。
     *
     * @param assetPath classpath 下的 SQL asset 路径
     * @return UTF-8 解码后的 SQL 模板内容
     */
    public static String load(String assetPath) {
        if (assetPath == null || assetPath.isBlank()) {
            throw new IllegalArgumentException("SQL asset path is required");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(assetPath)) {
            if (stream == null) {
                throw new IllegalArgumentException("SQL asset not found: " + assetPath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read SQL asset: " + assetPath, ex);
        }
    }

    /**
     * 使用环境配置渲染 SQL 模板占位符。
     *
     * <p>只解析形如 {@code ${PLACEHOLDER}} 的大写占位符；缺少值会直接失败，避免把未替换变量提交给 Flink。
     *
     * @param template SQL 模板内容
     * @param placeholders 占位符名称到替换值的映射
     * @return 渲染后的 Flink SQL
     */
    public static String render(String template, Map<String, String> placeholders) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (template == null) {
            throw new IllegalArgumentException("SQL template is required");
        }
        Map<String, String> values = placeholders == null ? Map.of() : placeholders;
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder rendered = new StringBuilder();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!values.containsKey(key)) {
                throw new IllegalArgumentException("Missing SQL placeholder value: " + key);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(values.get(key)));
        }
        matcher.appendTail(rendered);
        Matcher remaining = PLACEHOLDER.matcher(rendered);
        if (remaining.find()) {
            throw new IllegalArgumentException("Unresolved SQL placeholder: " + remaining.group(1));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return rendered.toString();
    }
}
