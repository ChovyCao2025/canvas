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

/**
 * TemplateRenderService 参与 engine.template 场景的画布执行引擎处理。
 */
public class TemplateRenderService {

    private static final String MISSING_VARIABLE = "MISSING_VARIABLE";
    private static final String INVALID_FORMAT_DATE = "INVALID_FORMAT_DATE";
    private static final String MAX_RENDERED_LENGTH = "MAX_RENDERED_LENGTH";

    private final int maxRenderedLength;

    /**
     * 创建 TemplateRenderService 实例并注入 engine.template 场景依赖。
     * @param maxRenderedLength max rendered length 参数，用于 TemplateRenderService 流程中的校验、计算或对象转换。
     */
    public TemplateRenderService(int maxRenderedLength) {
        if (maxRenderedLength <= 0) {
            throw new IllegalArgumentException("maxRenderedLength must be positive");
        }
        this.maxRenderedLength = maxRenderedLength;
    }

    /**
     * 渲染模板并收集渲染错误。
     *
     * <p>支持变量、条件、循环和日期格式化；缺失变量或格式错误会记录到返回结果而不是中断渲染。输出超过最大长度时会截断，
     * 并返回长度错误，调用方可据此决定是否继续发送。
     *
     * @param template 模板文本，空值按空字符串处理
     * @param context 渲染上下文，作为根作用域提供变量
     * @return 渲染后的文本和错误列表
     */
    public RenderResult render(String template, Map<String, Object> context) {
        List<RenderError> errors = new ArrayList<>();
        String output = renderSection(template == null ? "" : template, new Scope(context, context), errors);
        if (output.length() > maxRenderedLength) {
            errors.add(new RenderError(MAX_RENDERED_LENGTH, "rendered template exceeded maximum length", null));
            output = output.substring(0, maxRenderedLength);
        }
        return new RenderResult(output, List.copyOf(errors));
    }

    /**
     * 渲染模板片段。
     *
     * @param template 模板片段
     * @param scope 当前作用域
     * @param errors 渲染错误收集列表
     * @return 渲染后的片段文本
     */
    private String renderSection(String template, Scope scope, List<RenderError> errors) {
        StringBuilder output = new StringBuilder();
        int cursor = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (cursor < template.length()) {
            int open = template.indexOf("{{", cursor);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return output.toString();
    }

    /**
     * 渲染单个表达式标签。
     *
     * @param expression 标签表达式
     * @param scope 当前作用域
     * @param errors 渲染错误收集列表
     * @return HTML 转义后的表达式结果
     */
    private String renderExpression(String expression, Scope scope, List<RenderError> errors) {
        if (expression.startsWith("formatDate ")) {
            return escapeHtml(formatDate(expression, scope, errors));
        }
        Object value = resolve(expression, scope, errors);
        return value == null ? "" : escapeHtml(String.valueOf(value));
    }

    /**
     * 渲染 formatDate 表达式。
     *
     * @param expression formatDate 表达式
     * @param scope 当前作用域
     * @param errors 渲染错误收集列表
     * @return 格式化后的日期字符串
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            errors.add(new RenderError(INVALID_FORMAT_DATE, "could not format date field " + field, field));
            return "";
        }
    }

    /**
     * 从当前作用域解析变量路径。
     *
     * @param path 变量路径
     * @param scope 当前作用域
     * @param errors 渲染错误收集列表
     * @return 解析结果，缺失时记录错误并返回 null
     */
    private Object resolve(String path, Scope scope, List<RenderError> errors) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        Object value;
        if ("this".equals(normalized)) {
            value = scope.current();
        // 根据前序判断结果进入后续条件分支。
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

    /**
     * 从对象中读取点号路径。
     *
     * @param source 根对象
     * @param path 点号路径，支持 Map 和 List 数字下标
     * @return 路径值，缺失时返回 MissingValue
     */
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
            // 根据前序判断结果进入后续条件分支。
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

    /**
     * 在模板中查找匹配的块结束标签。
     *
     * @param template 模板文本
     * @param bodyStart 块正文开始位置
     * @param blockName 块名称
     * @return 块正文和结束位置，未找到时返回 null
     */
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
            // 根据前序判断结果进入后续条件分支。
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

    /**
     * 按空白拆分表达式，保留引号内空白。
     *
     * @param expression 表达式文本
     * @return token 列表
     */
    private static List<String> tokenize(String expression) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return parts;
    }

    /**
     * 将任意值转换为可迭代对象。
     *
     * @param value 原始值
     * @return Iterable，null 或不可迭代值返回空列表
     */
    private static Iterable<?> iterable(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (int i = 0; i < Array.getLength(value); i++) {
                result.add(Array.get(value, i));
            }
            return result;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Collections.emptyList();
    }

    /**
     * 判断模板条件中的 truthy 语义。
     *
     * @param value 原始值
     * @return true 表示条件成立
     */
    private static boolean truthy(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return true;
    }

    /**
     * HTML 转义渲染输出。
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * RenderResult 校验或转换 engine.template 场景的数据。
     * @param output output 参数，用于 RenderResult 流程中的校验、计算或对象转换。
     * @param errors errors 参数，用于 RenderResult 流程中的校验、计算或对象转换。
     * @return 返回 RenderResult 流程生成的业务结果。
     */
    public record RenderResult(String output, List<RenderError> errors) {}

    /**
     * RenderError 校验或转换 engine.template 场景的数据。
     * @param code 业务编码，用于匹配对应类型或状态。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param path path 参数，用于 RenderError 流程中的校验、计算或对象转换。
     * @return 返回 RenderError 流程生成的业务结果。
     */
    public record RenderError(String code, String message, String path) {}

    /**
     * 模板渲染作用域。
     *
     * @param root 根上下文
     * @param current 当前循环项或根上下文
     */
    private record Scope(Object root, Object current) {}

    /**
     * 模板块解析结果。
     *
     * @param body 块正文
     * @param afterEnd 块结束标签后的游标位置
     */
    private record Block(String body, int afterEnd) {}

    /**
     * 模板变量缺失哨兵值。
     */
    private enum MissingValue {
        INSTANCE
    }
}
