package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.IdentityTypeDO;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.IdentityTypeMapper;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class CdpAudienceSourceService {

    private static final String SOURCE_CDP_TAG = "CDP_TAG";
    private static final String SOURCE_CDP_PROFILE = "CDP_PROFILE";
    private static final String SOURCE_CDP_IDENTITY = "CDP_IDENTITY";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CdpUserTagMapper userTagMapper;
    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;
    private final TagDefinitionMapper tagDefinitionMapper;
    private final IdentityTypeMapper identityTypeMapper;
    private final ObjectMapper objectMapper;

    public List<AudienceSourceFieldDTO> listSourceFields(String dataSourceType) {
        return switch (normalizeSourceType(dataSourceType)) {
            case SOURCE_CDP_TAG -> tagDefinitionMapper.selectList(new LambdaQueryWrapper<TagDefinitionDO>()
                            .eq(TagDefinitionDO::getEnabled, 1)
                            .orderByAsc(TagDefinitionDO::getId))
                    .stream()
                    .map(tag -> new AudienceSourceFieldDTO(
                            tag.getTagCode(),
                            blankToFallback(tag.getName(), tag.getTagCode()),
                            blankToFallback(tag.getValueType(), "STRING")))
                    .toList();
            case SOURCE_CDP_PROFILE -> profileSourceFields();
            case SOURCE_CDP_IDENTITY -> identityTypeMapper.selectList(new LambdaQueryWrapper<IdentityTypeDO>()
                            .eq(IdentityTypeDO::getEnabled, 1)
                            .orderByAsc(IdentityTypeDO::getPriority)
                            .orderByAsc(IdentityTypeDO::getId))
                    .stream()
                    .map(type -> new AudienceSourceFieldDTO(
                            normalizeIdentityType(type.getCode()),
                            blankToFallback(type.getName(), type.getCode()),
                            "STRING"))
                    .toList();
            default -> throw new IllegalArgumentException("Unsupported CDP audience source: " + dataSourceType);
        };
    }

    public List<String> resolveUserIds(String dataSourceType, String ruleJson) {
        RuleGroup rule = parseRule(ruleJson);
        Set<String> userIds = new LinkedHashSet<>();
        switch (normalizeSourceType(dataSourceType)) {
            case SOURCE_CDP_TAG -> {
                Set<String> enabledTagCodes = enabledTagCodes();
                Map<String, Map<String, Object>> factsByUser = new LinkedHashMap<>();
                userTagMapper.selectList(new LambdaQueryWrapper<CdpUserTagDO>()
                                .eq(CdpUserTagDO::getStatus, STATUS_ACTIVE))
                        .forEach(tag -> {
                            if (tag.getUserId() != null
                                    && tag.getTagCode() != null
                                    && enabledTagCodes.contains(tag.getTagCode())) {
                                factsByUser.computeIfAbsent(tag.getUserId(), ignored -> new LinkedHashMap<>())
                                        .put(tag.getTagCode(), tag.getTagValue());
                            }
                        });
                factsByUser.entrySet().stream()
                        .filter(entry -> matchesFacts(rule, entry.getValue()))
                        .map(Map.Entry::getKey)
                        .forEach(userIds::add);
            }
            case SOURCE_CDP_PROFILE -> profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                            .eq(CdpUserProfileDO::getStatus, STATUS_ACTIVE))
                    .stream()
                    .filter(profile -> matchesProfile(rule, profile))
                    .map(CdpUserProfileDO::getUserId)
                    .filter(Objects::nonNull)
                    .forEach(userIds::add);
            case SOURCE_CDP_IDENTITY -> {
                Set<String> enabledIdentityTypes = enabledIdentityTypes();
                Map<String, Map<String, Object>> factsByUser = new LinkedHashMap<>();
                identityMapper.selectList(new LambdaQueryWrapper<CdpUserIdentityDO>())
                        .forEach(identity -> {
                            String identityType = normalizeIdentityType(identity.getIdentityType());
                            if (identity.getUserId() != null && enabledIdentityTypes.contains(identityType)) {
                                addMultiValueFact(
                                        factsByUser.computeIfAbsent(identity.getUserId(), ignored -> new LinkedHashMap<>()),
                                        identityType,
                                        identity.getIdentityValue());
                            }
                        });
                factsByUser.entrySet().stream()
                        .filter(entry -> matchesFacts(rule, entry.getValue()))
                        .map(Map.Entry::getKey)
                        .forEach(userIds::add);
            }
            default -> throw new IllegalArgumentException("Unsupported CDP audience source: " + dataSourceType);
        }
        return List.copyOf(userIds);
    }

    private Set<String> enabledTagCodes() {
        Set<String> codes = new LinkedHashSet<>();
        tagDefinitionMapper.selectList(new LambdaQueryWrapper<TagDefinitionDO>()
                        .eq(TagDefinitionDO::getEnabled, 1))
                .forEach(tag -> {
                    if (tag.getTagCode() != null && !tag.getTagCode().isBlank()) {
                        codes.add(tag.getTagCode());
                    }
                });
        return codes;
    }

    private Set<String> enabledIdentityTypes() {
        Set<String> types = new LinkedHashSet<>();
        identityTypeMapper.selectList(new LambdaQueryWrapper<IdentityTypeDO>()
                        .eq(IdentityTypeDO::getEnabled, 1))
                .forEach(type -> {
                    String code = normalizeIdentityType(type.getCode());
                    if (!code.isBlank()) {
                        types.add(code);
                    }
                });
        return types;
    }

    public boolean supports(String dataSourceType) {
        String source = normalizeSourceType(dataSourceType);
        return SOURCE_CDP_TAG.equals(source)
                || SOURCE_CDP_PROFILE.equals(source)
                || SOURCE_CDP_IDENTITY.equals(source);
    }

    private List<AudienceSourceFieldDTO> profileSourceFields() {
        Map<String, AudienceSourceFieldDTO> fields = new LinkedHashMap<>();
        fields.put("displayName", new AudienceSourceFieldDTO("displayName", "展示名", "STRING"));
        fields.put("phone", new AudienceSourceFieldDTO("phone", "手机号", "STRING"));
        fields.put("email", new AudienceSourceFieldDTO("email", "邮箱", "STRING"));
        fields.put("status", new AudienceSourceFieldDTO("status", "状态", "STRING"));
        fields.put("firstSeenAt", new AudienceSourceFieldDTO("firstSeenAt", "首次出现时间", "STRING"));
        fields.put("lastSeenAt", new AudienceSourceFieldDTO("lastSeenAt", "最近活跃时间", "STRING"));
        profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                        .eq(CdpUserProfileDO::getStatus, STATUS_ACTIVE))
                .stream()
                .map(CdpUserProfileDO::getPropertiesJson)
                .map(this::parseProperties)
                .forEach(props -> props.forEach((key, value) ->
                        fields.putIfAbsent(key, new AudienceSourceFieldDTO(key, key, inferValueType(value)))));
        return List.copyOf(fields.values());
    }

    private boolean matchesProfile(RuleGroup rule, CdpUserProfileDO profile) {
        Map<String, Object> facts = new LinkedHashMap<>(parseProperties(profile.getPropertiesJson()));
        facts.put("displayName", profile.getDisplayName());
        facts.put("phone", profile.getPhone());
        facts.put("email", profile.getEmail());
        facts.put("status", profile.getStatus());
        facts.put("firstSeenAt", profile.getFirstSeenAt() == null ? null : profile.getFirstSeenAt().toString());
        facts.put("lastSeenAt", profile.getLastSeenAt() == null ? null : profile.getLastSeenAt().toString());
        return matchesFacts(rule, facts);
    }

    private boolean matchesFacts(RuleGroup rule, Map<String, Object> facts) {
        return evaluateGroup(rule, condition -> compare(facts.get(condition.field()), condition.op(), condition.value()));
    }

    private boolean evaluateGroup(RuleGroup group, Predicate<RuleCondition> predicate) {
        if (group.conditions().isEmpty() && group.groups().isEmpty()) {
            return true;
        }
        boolean or = "OR".equalsIgnoreCase(group.logic());
        boolean result = !or;
        for (RuleCondition condition : group.conditions()) {
            boolean current = predicate.test(condition);
            result = or ? result || current : result && current;
        }
        for (RuleGroup child : group.groups()) {
            boolean current = evaluateGroup(child, predicate);
            result = or ? result || current : result && current;
        }
        return result;
    }

    private boolean compare(Object actual, String op, Object expected) {
        if (actual == null) {
            return false;
        }
        String normalizedOp = op == null ? "" : op.trim();
        if (actual instanceof Collection<?> values) {
            return compareCollection(values, normalizedOp, expected);
        }
        return switch (normalizedOp) {
            case "=" -> Objects.equals(stringValue(actual), stringValue(expected));
            case "!=" -> !Objects.equals(stringValue(actual), stringValue(expected));
            case ">" -> compareNumbers(actual, expected, comparison -> comparison > 0);
            case ">=" -> compareNumbers(actual, expected, comparison -> comparison >= 0);
            case "<" -> compareNumbers(actual, expected, comparison -> comparison < 0);
            case "<=" -> compareNumbers(actual, expected, comparison -> comparison <= 0);
            case "IN", "in" -> expected instanceof List<?> list
                    && list.stream().map(this::stringValue).anyMatch(item -> item.equals(stringValue(actual)));
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    private boolean compareCollection(Collection<?> actualValues, String op, Object expected) {
        List<?> values = actualValues.stream()
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return false;
        }
        return switch (op) {
            case "=" -> values.stream().anyMatch(actual -> Objects.equals(stringValue(actual), stringValue(expected)));
            case "!=" -> values.stream().noneMatch(actual -> Objects.equals(stringValue(actual), stringValue(expected)));
            case ">" -> values.stream().anyMatch(actual -> compareNumbers(actual, expected, comparison -> comparison > 0));
            case ">=" -> values.stream().anyMatch(actual -> compareNumbers(actual, expected, comparison -> comparison >= 0));
            case "<" -> values.stream().anyMatch(actual -> compareNumbers(actual, expected, comparison -> comparison < 0));
            case "<=" -> values.stream().anyMatch(actual -> compareNumbers(actual, expected, comparison -> comparison <= 0));
            case "IN", "in" -> values.stream().anyMatch(actual -> matchesIn(actual, expected));
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    private boolean matchesIn(Object actual, Object expected) {
        return expected instanceof List<?> list
                && list.stream().map(this::stringValue).anyMatch(item -> item.equals(stringValue(actual)));
    }

    private boolean compareNumbers(Object actual, Object expected, IntPredicate predicate) {
        BigDecimal actualNumber = numberOrNull(actual);
        if (actualNumber == null) {
            return false;
        }
        return predicate.test(actualNumber.compareTo(number(expected)));
    }

    @SuppressWarnings("unchecked")
    private void addMultiValueFact(Map<String, Object> facts, String field, Object value) {
        Object existing = facts.get(field);
        if (existing instanceof List<?> list) {
            ((List<Object>) list).add(value);
            return;
        }
        List<Object> values = new ArrayList<>();
        if (existing != null) {
            values.add(existing);
        }
        values.add(value);
        facts.put(field, values);
    }

    private RuleGroup parseRule(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) {
            return new RuleGroup("AND", List.of(), List.of());
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(ruleJson, new TypeReference<>() {});
            return toGroup(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid audience ruleJson", e);
        }
    }

    @SuppressWarnings("unchecked")
    private RuleGroup toGroup(Map<String, Object> raw) {
        String logic = String.valueOf(raw.getOrDefault("logic", "AND")).toUpperCase(Locale.ROOT);
        List<RuleCondition> conditions = ((List<Object>) raw.getOrDefault("conditions", List.of()))
                .stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<String, Object> condition = (Map<String, Object>) item;
                    return new RuleCondition(
                            String.valueOf(condition.get("field")),
                            String.valueOf(condition.get("op")),
                            condition.get("value"));
                })
                .toList();
        List<RuleGroup> groups = ((List<Object>) raw.getOrDefault("groups", List.of()))
                .stream()
                .filter(Map.class::isInstance)
                .map(item -> toGroup((Map<String, Object>) item))
                .toList();
        return new RuleGroup(logic, conditions, groups);
    }

    private Map<String, Object> parseProperties(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private BigDecimal number(Object value) {
        try {
            return new BigDecimal(stringValue(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Audience condition value must be numeric: " + value);
        }
    }

    private BigDecimal numberOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(stringValue(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeSourceType(String dataSourceType) {
        return dataSourceType == null ? "" : dataSourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeIdentityType(String identityType) {
        return identityType == null ? "" : identityType.trim().toUpperCase(Locale.ROOT);
    }

    private String inferValueType(Object value) {
        if (value instanceof Number) {
            return "NUMBER";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        return "STRING";
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record RuleGroup(String logic, List<RuleCondition> conditions, List<RuleGroup> groups) {}

    private record RuleCondition(String field, String op, Object value) {}
}
