package org.chovy.canvas.marketing.adapter.persistence;

import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignLink;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责MarketingCampaignPersistenceConverter相关对象之间的字段转换。
 */
@Component
public class MarketingCampaignPersistenceConverter {

    /**
     * 转换为campaignRow对象。
     */
    public MarketingCampaignMasterDO toCampaignRow(MarketingCampaign campaign) {
        if (campaign == null) {
            return null;
        }
        MarketingCampaignMasterDO row = new MarketingCampaignMasterDO();
        row.setId(campaign.id());
        row.setTenantId(campaign.tenantId());
        row.setCampaignKey(campaign.campaignKey().value());
        row.setCampaignName(campaign.campaignName());
        row.setObjective(campaign.objective());
        row.setStatus(campaign.status().name());
        row.setPrimaryChannel(campaign.primaryChannel());
        row.setOwnerTeam(campaign.ownerTeam());
        row.setStartAt(campaign.dateRange().startAt());
        row.setEndAt(campaign.dateRange().endAt());
        row.setBudgetAmount(campaign.budget().amount());
        row.setCurrency(campaign.budget().currency());
        row.setBriefJson(SimpleJsonMapCodec.toJson(campaign.brief()));
        row.setCreatedBy(campaign.createdBy());
        row.setUpdatedBy(campaign.updatedBy());
        row.setCreatedAt(campaign.createdAt());
        row.setUpdatedAt(campaign.updatedAt());
        return row;
    }

    /**
     * 转换为campaign对象。
     */
    public MarketingCampaign toCampaign(MarketingCampaignMasterDO row) {
        if (row == null) {
            return null;
        }
        return MarketingCampaign.createExisting(
                row.getId(),
                row.getTenantId(),
                CampaignKey.of(row.getCampaignKey(), "campaignKey"),
                row.getCampaignName(),
                row.getObjective(),
                CampaignStatus.from(row.getStatus()),
                row.getPrimaryChannel(),
                row.getOwnerTeam(),
                row.getStartAt(),
                row.getEndAt(),
                row.getBudgetAmount(),
                row.getCurrency(),
                SimpleJsonMapCodec.fromJson(row.getBriefJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为linkRow对象。
     */
    public MarketingCampaignLinkDO toLinkRow(MarketingCampaignLink link) {
        if (link == null) {
            return null;
        }
        MarketingCampaignLinkDO row = new MarketingCampaignLinkDO();
        row.setId(link.id());
        row.setTenantId(link.tenantId());
        row.setCampaignId(link.campaignId());
        row.setResourceType(link.resourceType());
        row.setResourceId(link.resourceId());
        row.setResourceKey(link.resourceKey().value());
        row.setResourceName(link.resourceName());
        row.setResourceRoute(link.resourceRoute());
        row.setDependencyRole(link.dependencyRole());
        row.setLinkStatus(link.linkStatus().name());
        row.setRequiredForLaunch(link.requiredForLaunch() ? 1 : 0);
        row.setMetadataJson(SimpleJsonMapCodec.toJson(link.metadata()));
        row.setCreatedBy(link.createdBy());
        row.setUpdatedBy(link.updatedBy());
        row.setCreatedAt(link.createdAt());
        row.setUpdatedAt(link.updatedAt());
        return row;
    }

    /**
     * 转换为link对象。
     */
    public MarketingCampaignLink toLink(MarketingCampaignLinkDO row) {
        if (row == null) {
            return null;
        }
        return MarketingCampaignLink.createExisting(
                row.getId(),
                row.getTenantId(),
                row.getCampaignId(),
                row.getResourceType(),
                row.getResourceId(),
                CampaignKey.of(row.getResourceKey(), "resourceKey"),
                row.getResourceName(),
                row.getResourceRoute(),
                row.getDependencyRole(),
                row.getLinkStatus(),
                row.getRequiredForLaunch() != null && row.getRequiredForLaunch() == 1,
                SimpleJsonMapCodec.fromJson(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 提供SimpleJsonMapCodec的业务能力。
     */
    private static final class SimpleJsonMapCodec {

        /**
         * 创建SimpleJsonMapCodec实例。
         */
        private SimpleJsonMapCodec() {
        }

        static String toJson(Map<String, Object> value) {
            if (value == null || value.isEmpty()) {
                return "{}";
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(quote(entry.getKey())).append(':').append(valueToJson(entry.getValue()));
            }
            return builder.append('}').toString();
        }

        static Map<String, Object> fromJson(String value) {
            if (value == null || value.isBlank()) {
                return Map.of();
            }
            try {
                Parser parser = new Parser(value);
                Map<String, Object> result = parser.parseObject();
                parser.ensureComplete();
                return result;
            } catch (IllegalArgumentException ignored) {
                return Map.of();
            }
        }

        /**
         * 执行valueToJson业务操作。
         */
        private static String valueToJson(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof Map<?, ?> map) {
                return mapToJson(map);
            }
            if (value instanceof Iterable<?> iterable) {
                return iterableToJson(iterable);
            }
            if (value.getClass().isArray()) {
                return arrayToJson(value);
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            return quote(String.valueOf(value));
        }

        /**
         * 执行mapToJson业务操作。
         */
        private static String mapToJson(Map<?, ?> map) {
            if (map.isEmpty()) {
                return "{}";
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(quote(String.valueOf(entry.getKey()))).append(':').append(valueToJson(entry.getValue()));
            }
            return builder.append('}').toString();
        }

        /**
         * 执行iterableToJson业务操作。
         */
        private static String iterableToJson(Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(valueToJson(item));
            }
            return builder.append(']').toString();
        }

        /**
         * 执行arrayToJson业务操作。
         */
        private static String arrayToJson(Object array) {
            StringBuilder builder = new StringBuilder("[");
            for (int index = 0; index < Array.getLength(array); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                builder.append(valueToJson(Array.get(array, index)));
            }
            return builder.append(']').toString();
        }

        /**
         * 执行quote业务操作。
         */
        private static String quote(String value) {
            StringBuilder builder = new StringBuilder("\"");
            String safeValue = value == null ? "" : value;
            for (int index = 0; index < safeValue.length(); index++) {
                char current = safeValue.charAt(index);
                switch (current) {
                    case '"' -> builder.append("\\\"");
                    case '\\' -> builder.append("\\\\");
                    case '\b' -> builder.append("\\b");
                    case '\f' -> builder.append("\\f");
                    case '\n' -> builder.append("\\n");
                    case '\r' -> builder.append("\\r");
                    case '\t' -> builder.append("\\t");
                    default -> builder.append(current);
                }
            }
            return builder.append('"').toString();
        }

        /**
         * 提供Parser的业务能力。
         */
        private static final class Parser {
            /**
             * 保存text字段值。
             */
            private final String text;

            /**
             * 保存index字段值。
             */
            private int index;

            /**
             * 创建Parser实例。
             */
            private Parser(String text) {
                this.text = text.trim();
            }

            /**
             * 执行parseObject业务操作。
             */
            private Map<String, Object> parseObject() {
                LinkedHashMap<String, Object> result = new LinkedHashMap<>();
                expect('{');
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return Collections.unmodifiableMap(result);
                }
                while (index < text.length()) {
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    Object value = parseValue();
                    result.put(key, value);
                    skipWhitespace();
                    if (peek(',')) {
                        index++;
                        continue;
                    }
                    expect('}');
                    return Collections.unmodifiableMap(result);
                }
                throw new IllegalArgumentException("unterminated JSON object");
            }

            /**
             * 执行ensureComplete业务操作。
             */
            private void ensureComplete() {
                skipWhitespace();
                if (index != text.length()) {
                    throw new IllegalArgumentException("trailing JSON content");
                }
            }

            /**
             * 执行parseValue业务操作。
             */
            private Object parseValue() {
                skipWhitespace();
                if (peek('"')) {
                    return parseString();
                }
                if (peek('{')) {
                    return parseObject();
                }
                if (peek('[')) {
                    return parseArray();
                }
                if (match("true")) {
                    return Boolean.TRUE;
                }
                if (match("false")) {
                    return Boolean.FALSE;
                }
                if (match("null")) {
                    return null;
                }
                return parseNumber();
            }

            /**
             * 执行parseArray业务操作。
             */
            private List<Object> parseArray() {
                List<Object> result = new ArrayList<>();
                expect('[');
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return Collections.unmodifiableList(result);
                }
                while (index < text.length()) {
                    result.add(parseValue());
                    skipWhitespace();
                    if (peek(',')) {
                        index++;
                        continue;
                    }
                    expect(']');
                    return Collections.unmodifiableList(result);
                }
                throw new IllegalArgumentException("unterminated JSON array");
            }

            /**
             * 执行parseNumber业务操作。
             */
            private Number parseNumber() {
                int start = index;
                if (peek('-')) {
                    index++;
                }
                int integerStart = index;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
                if (integerStart == index) {
                    throw new IllegalArgumentException("invalid JSON number");
                }
                boolean decimal = false;
                if (peek('.')) {
                    decimal = true;
                    index++;
                    int fractionStart = index;
                    while (index < text.length() && Character.isDigit(text.charAt(index))) {
                        index++;
                    }
                    if (fractionStart == index) {
                        throw new IllegalArgumentException("invalid JSON number");
                    }
                }
                if (peek('e') || peek('E')) {
                    decimal = true;
                    index++;
                    if (peek('+') || peek('-')) {
                        index++;
                    }
                    int exponentStart = index;
                    while (index < text.length() && Character.isDigit(text.charAt(index))) {
                        index++;
                    }
                    if (exponentStart == index) {
                        throw new IllegalArgumentException("invalid JSON number");
                    }
                }
                String number = text.substring(start, index);
                if (decimal) {
                    return new java.math.BigDecimal(number);
                }
                long parsed = Long.parseLong(number);
                if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) {
                    return (int) parsed;
                }
                return parsed;
            }

            /**
             * 执行parseString业务操作。
             */
            private String parseString() {
                expect('"');
                StringBuilder builder = new StringBuilder();
                while (index < text.length()) {
                    char current = text.charAt(index++);
                    if (current == '"') {
                        return builder.toString();
                    }
                    if (current == '\\') {
                        if (index >= text.length()) {
                            throw new IllegalArgumentException("invalid JSON escape");
                        }
                        char escaped = text.charAt(index++);
                        switch (escaped) {
                            case '"' -> builder.append('"');
                            case '\\' -> builder.append('\\');
                            case '/' -> builder.append('/');
                            case 'b' -> builder.append('\b');
                            case 'f' -> builder.append('\f');
                            case 'n' -> builder.append('\n');
                            case 'r' -> builder.append('\r');
                            case 't' -> builder.append('\t');
                            default -> throw new IllegalArgumentException("unsupported JSON escape");
                        }
                    } else {
                        builder.append(current);
                    }
                }
                throw new IllegalArgumentException("unterminated JSON string");
            }

            /**
             * 执行match业务操作。
             */
            private boolean match(String token) {
                if (!text.startsWith(token, index)) {
                    return false;
                }
                index += token.length();
                return true;
            }

            /**
             * 执行expect业务操作。
             */
            private void expect(char expected) {
                skipWhitespace();
                if (!peek(expected)) {
                    throw new IllegalArgumentException("expected " + expected);
                }
                index++;
            }

            /**
             * 执行peek业务操作。
             */
            private boolean peek(char expected) {
                return index < text.length() && text.charAt(index) == expected;
            }

            /**
             * 执行skipWhitespace业务操作。
             */
            private void skipWhitespace() {
                while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                    index++;
                }
            }
        }
    }
}
