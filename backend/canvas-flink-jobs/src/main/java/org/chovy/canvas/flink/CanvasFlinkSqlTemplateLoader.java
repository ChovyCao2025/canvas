package org.chovy.canvas.flink;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CanvasFlinkSqlTemplateLoader {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z0-9_]+)}");

    private CanvasFlinkSqlTemplateLoader() {
    }

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
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read SQL asset: " + assetPath, ex);
        }
    }

    public static String render(String template, Map<String, String> placeholders) {
        if (template == null) {
            throw new IllegalArgumentException("SQL template is required");
        }
        Map<String, String> values = placeholders == null ? Map.of() : placeholders;
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder rendered = new StringBuilder();
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
        return rendered.toString();
    }
}
