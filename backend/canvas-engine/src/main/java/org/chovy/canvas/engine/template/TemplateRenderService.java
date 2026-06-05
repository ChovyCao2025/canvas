package org.chovy.canvas.engine.template;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TemplateRenderService {

    private static final String MISSING_VARIABLE = "MISSING_VARIABLE";
    private static final String INVALID_FORMAT_DATE = "INVALID_FORMAT_DATE";
    private static final String MAX_RENDERED_LENGTH = "MAX_RENDERED_LENGTH";

    private final int maxRenderedLength;

    public TemplateRenderService(int maxRenderedLength) {
        if (maxRenderedLength <= 0) {
            throw new IllegalArgumentException("maxRenderedLength must be positive");
        }
        this.maxRenderedLength = maxRenderedLength;
    }

    public RenderResult render(String template, Map<String, Object> context) {
        List<RenderError> errors = new ArrayList<>();
        String output = renderSection(template == null ? "" : template, new Scope(context, context), errors);
        if (output.length() > maxRenderedLength) {
            errors.add(new RenderError(MAX_RENDERED_LENGTH, "rendered template exceeded maximum length", null));
            output = output.substring(0, maxRenderedLength);
        }
        return new RenderResult(output, List.copyOf(errors));
    }

    private String renderSection(String template, Scope scope, List<RenderError> errors) {
        StringBuilder output = new StringBuilder();
        int cursor = 0;
        while (cursor < template.length()) {
            int open = template.indexOf("{{", cursor);
            if (open < 0) {
                output.append(template, cursor, template.length());
                break;
            }
            output.append(template, cursor, open);
            int close = template.indexOf("}}", open + 2);
            if (close < 0) {
                output.append(template, open, template.length());
                break;
            }

            String tag = template.substring(open + 2, close).trim();
            if (tag.startsWith("#if ")) {
                Block block = findBlock(template, close + 2, "if");
                if (block == null) {
                    cursor = close + 2;
                    continue;
                }
                if (truthy(resolve(tag.substring(4).trim(), scope, errors))) {
                    output.append(renderSection(block.body(), scope, errors));
                }
                cursor = block.afterEnd();
                continue;
            }
            if (tag.startsWith("#each ")) {
                Block block = findBlock(template, close + 2, "each");
                if (block == null) {
                    cursor = close + 2;
                    continue;
                }
                Object value = resolve(tag.substring(6).trim(), scope, errors);
                for (Object item : iterable(value)) {
                    output.append(renderSection(block.body(), new Scope(scope.root(), item), errors));
                }
                cursor = block.afterEnd();
                continue;
            }
            if (!tag.startsWith("/")) {
                output.append(renderExpression(tag, scope, errors));
            }
            cursor = close + 2;
        }
        return output.toString();
    }

    private String renderExpression(String expression, Scope scope, List<RenderError> errors) {
        if (expression.startsWith("formatDate ")) {
            return escapeHtml(formatDate(expression, scope, errors));
        }
        Object value = resolve(expression, scope, errors);
        return value == null ? "" : escapeHtml(String.valueOf(value));
    }

    private String formatDate(String expression, Scope scope, List<RenderError> errors) {
        List<String> parts = tokenize(expression);
        if (parts.size() < 3) {
            errors.add(new RenderError(INVALID_FORMAT_DATE, "formatDate requires a field and pattern", expression));
            return "";
        }
        String field = parts.get(1);
        String pattern = parts.get(2);
        Object value = resolve(field, scope, errors);
        if (value == null) {
            return "";
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
            if (value instanceof TemporalAccessor temporal) {
                return formatter.format(temporal);
            }
            String raw = String.valueOf(value);
            if (raw.endsWith("Z")) {
                return formatter.format(Instant.parse(raw));
            }
            if (raw.contains("+")) {
                return formatter.format(OffsetDateTime.parse(raw));
            }
            if (raw.length() == 10) {
                return LocalDate.parse(raw).format(DateTimeFormatter.ofPattern(pattern));
            }
            return LocalDateTime.parse(raw).format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            errors.add(new RenderError(INVALID_FORMAT_DATE, "could not format date field " + field, field));
            return "";
        }
    }

    private Object resolve(String path, Scope scope, List<RenderError> errors) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        Object value;
        if ("this".equals(normalized)) {
            value = scope.current();
        } else if (normalized.startsWith("this.")) {
            value = readPath(scope.current(), normalized.substring("this.".length()));
        } else {
            value = readPath(scope.current(), normalized);
            if (value == MissingValue.INSTANCE) {
                value = readPath(scope.root(), normalized);
            }
        }
        if (value == MissingValue.INSTANCE) {
            errors.add(new RenderError(MISSING_VARIABLE, "missing template variable " + normalized, normalized));
            return null;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Object readPath(Object source, String path) {
        if (source == null || path == null || path.isBlank()) {
            return MissingValue.INSTANCE;
        }
        Object current = source;
        for (String segment : path.split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                if (!map.containsKey(segment)) {
                    return MissingValue.INSTANCE;
                }
                current = ((Map<String, Object>) map).get(segment);
            } else if (current instanceof List<?> list && segment.matches("\\d+")) {
                int index = Integer.parseInt(segment);
                if (index < 0 || index >= list.size()) {
                    return MissingValue.INSTANCE;
                }
                current = list.get(index);
            } else {
                return MissingValue.INSTANCE;
            }
        }
        return current;
    }

    private Block findBlock(String template, int bodyStart, String blockName) {
        int depth = 1;
        int cursor = bodyStart;
        while (cursor < template.length()) {
            int open = template.indexOf("{{", cursor);
            if (open < 0) {
                return null;
            }
            int close = template.indexOf("}}", open + 2);
            if (close < 0) {
                return null;
            }
            String tag = template.substring(open + 2, close).trim();
            if (tag.startsWith("#" + blockName + " ")) {
                depth++;
            } else if (tag.equals("/" + blockName)) {
                depth--;
                if (depth == 0) {
                    return new Block(template.substring(bodyStart, open), close + 2);
                }
            }
            cursor = close + 2;
        }
        return null;
    }

    private static List<String> tokenize(String expression) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if ((ch == '\'' || ch == '"') && quote == 0) {
                quote = ch;
                continue;
            }
            if (ch == quote) {
                quote = 0;
                continue;
            }
            if (Character.isWhitespace(ch) && quote == 0) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }

    private static Iterable<?> iterable(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                result.add(Array.get(value, i));
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0.0d;
        }
        if (value instanceof String string) {
            return !string.isBlank() && !"false".equalsIgnoreCase(string);
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable.iterator().hasNext();
        }
        return true;
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record RenderResult(String output, List<RenderError> errors) {}

    public record RenderError(String code, String message, String path) {}

    private record Scope(Object root, Object current) {}

    private record Block(String body, int afterEnd) {}

    private enum MissingValue {
        INSTANCE
    }
}
