package org.chovy.canvas.canvas.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装JsonSupport相关的业务逻辑。
 */
final class JsonSupport {

    /**
     * 创建当前对象实例。
     */
    private JsonSupport() {
    }

    /**
     * 处理parse。
     */
    static Object parse(String json) {
        return new Parser(json).parse();
    }

    /**
     * 处理parseObject。
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(String json) {
        Object parsed = parse(json);
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("JSON root must be an object");
    }

    /**
     * 转换为Json。
     */
    static String toJson(Object value) {
        StringBuilder out = new StringBuilder();
        writeJson(value, out);
        return out.toString();
    }

    /**
     * 处理writeJSON 内容。
     */
    @SuppressWarnings("unchecked")
    private static void writeJson(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            writeString(text, out);
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                writeString(String.valueOf(entry.getKey()), out);
                out.append(':');
                writeJson(entry.getValue(), out);
                first = false;
            }
            out.append('}');
        } else if (value instanceof Iterable<?> values) {
            out.append('[');
            boolean first = true;
            for (Object element : values) {
                if (!first) {
                    out.append(',');
                }
                writeJson(element, out);
                first = false;
            }
            out.append(']');
        } else if (value.getClass().isArray()) {
            out.append('[');
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                writeJson(java.lang.reflect.Array.get(value, i), out);
            }
            out.append(']');
        } else {
            writeString(String.valueOf(value), out);
        }
    }

    /**
     * 处理writeString。
     */
    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
    }

    /**
     * 封装Parser相关的业务逻辑。
     */
    private static final class Parser {

        /**
         * 保存text。
         */
        private final String text;

        /**
         * 保存index。
         */
        private int index;

        /**
         * 创建当前对象实例。
         */
        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        /**
         * 处理parse。
         */
        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("Unexpected trailing JSON");
            }
            return value;
        }

        /**
         * 处理parseValue。
         */
        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("Unexpected end of JSON");
            }
            char ch = text.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected JSON token");
                }
            };
        }

        /**
         * 处理parseObject。
         */
        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return result;
                }
                expect(',');
            }
        }

        /**
         * 处理parseArray。
         */
        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return result;
                }
                expect(',');
            }
        }

        /**
         * 处理parseString。
         */
        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return result.toString();
                }
                if (ch != '\\') {
                    result.append(ch);
                    continue;
                }
                if (index >= text.length()) {
                    throw error("Unterminated JSON escape");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(parseUnicodeEscape());
                    default -> throw error("Invalid JSON escape");
                }
            }
            throw error("Unterminated JSON string");
        }

        /**
         * 处理parseUnicodeEscape。
         */
        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) {
                throw error("Invalid unicode escape");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw error("Invalid unicode escape");
            }
        }

        /**
         * 处理parseLiteral。
         */
        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Invalid JSON literal");
            }
            index += literal.length();
            return value;
        }

        /**
         * 处理parseNumber。
         */
        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            readDigits();
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                readDigits();
            }
            if (peek('e') || peek('E')) {
                decimal = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                readDigits();
            }
            String number = text.substring(start, index);
            try {
                if (decimal) {
                    return Double.parseDouble(number);
                }
                long parsed = Long.parseLong(number);
                if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) {
                    return (int) parsed;
                }
                return parsed;
            } catch (NumberFormatException ex) {
                throw error("Invalid JSON number");
            }
        }

        /**
         * 处理readDigits。
         */
        private void readDigits() {
            int start = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Expected JSON digit");
            }
        }

        /**
         * 处理skipWhitespace。
         */
        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        /**
         * 处理peek。
         */
        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        /**
         * 处理expect。
         */
        private void expect(char expected) {
            if (!peek(expected)) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        /**
         * 处理error。
         */
        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at JSON offset " + index);
        }
    }
}
