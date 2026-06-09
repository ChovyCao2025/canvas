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

/**
 * CdpAudienceSourceService 参与 engine.audience 场景的画布执行引擎处理。
 */
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

    /**
     * 列出指定 CDP 人群来源可用于规则配置的字段。
     *
     * <p>方法按来源类型读取标签定义、画像字段或身份类型，不计算人群用户；返回字段编码、展示名和类型供前端规则编辑器使用。
     *
     * @param dataSourceType 数据来源类型，支持 CDP_TAG、CDP_PROFILE、CDP_IDENTITY
     * @return 可配置字段列表
     */
    public List<AudienceSourceFieldDTO> listSourceFields(String dataSourceType) {
        return switch (normalizeSourceType(dataSourceType)) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            case SOURCE_CDP_TAG -> tagDefinitionMapper.selectList(new LambdaQueryWrapper<TagDefinitionDO>()
                            .eq(TagDefinitionDO::getEnabled, 1)
                            .orderByAsc(TagDefinitionDO::getId))
                    // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 根据规则 JSON 解析并计算命中的 CDP 用户 ID。
     *
     * <p>方法会读取对应 CDP 表数据并在内存中执行规则匹配；标签和身份来源会过滤未启用定义，画像来源会合并属性 JSON。
     * 返回值用于人群快照或批量计算，不写 Redis 或数据库。
     *
     * @param dataSourceType 数据来源类型
     * @param ruleJson 规则 JSON
     * @return 去重后按发现顺序排列的用户 ID
     */
    public List<String> resolveUserIds(String dataSourceType, String ruleJson) {
        RuleGroup rule = parseRule(ruleJson);
        Set<String> userIds = new LinkedHashSet<>();
        switch (normalizeSourceType(dataSourceType)) {
            case SOURCE_CDP_TAG -> {
                Set<String> enabledTagCodes = enabledTagCodes();
                Map<String, Map<String, Object>> factsByUser = new LinkedHashMap<>();
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                userTagMapper.selectList(new LambdaQueryWrapper<CdpUserTagDO>()
                                .eq(CdpUserTagDO::getStatus, STATUS_ACTIVE))
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .forEach(tag -> {
                            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 查询当前启用的 CDP 标签编码集合。
     *
     * @return 启用标签编码集合
     */
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

    /**
     * 查询当前启用的身份类型集合。
     *
     * @return 归一化后的身份类型集合
     */
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

    /**
     * 判断当前服务是否支持指定 CDP 人群来源。
     *
     * <p>方法仅做类型归一化和枚举判断，不访问数据库。
     *
     * @param dataSourceType 数据来源类型
     * @return {@code true} 表示可由本服务解析字段和用户
     */
    public boolean supports(String dataSourceType) {
        String source = normalizeSourceType(dataSourceType);
        return SOURCE_CDP_TAG.equals(source)
                || SOURCE_CDP_PROFILE.equals(source)
                || SOURCE_CDP_IDENTITY.equals(source);
    }

    /**
     * 生成 CDP 画像来源可配置字段。
     *
     * @return 画像基础字段和属性 JSON 动态字段列表
     */
    private List<AudienceSourceFieldDTO> profileSourceFields() {
        Map<String, AudienceSourceFieldDTO> fields = new LinkedHashMap<>();
        fields.put("displayName", new AudienceSourceFieldDTO("displayName", "展示名", "STRING"));
        fields.put("phone", new AudienceSourceFieldDTO("phone", "手机号", "STRING"));
        fields.put("email", new AudienceSourceFieldDTO("email", "邮箱", "STRING"));
        fields.put("status", new AudienceSourceFieldDTO("status", "状态", "STRING"));
        fields.put("firstSeenAt", new AudienceSourceFieldDTO("firstSeenAt", "首次出现时间", "STRING"));
        fields.put("lastSeenAt", new AudienceSourceFieldDTO("lastSeenAt", "最近活跃时间", "STRING"));
        fields.put("churn_probability", new AudienceSourceFieldDTO("churn_probability", "流失概率", "NUMBER"));
        fields.put("churn_risk_band", new AudienceSourceFieldDTO("churn_risk_band", "流失风险", "STRING"));
        fields.put("best_send_hour", new AudienceSourceFieldDTO("best_send_hour", "最佳发送小时", "NUMBER"));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                        .eq(CdpUserProfileDO::getStatus, STATUS_ACTIVE))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(CdpUserProfileDO::getPropertiesJson)
                .map(this::parseProperties)
                .forEach(props -> props.forEach((key, value) ->
                        fields.putIfAbsent(key, new AudienceSourceFieldDTO(key, key, inferValueType(value)))));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(fields.values());
    }

    /**
     * 判断单个画像是否命中规则。
     *
     * @param rule 规则组
     * @param profile 用户画像记录
     * @return true 表示该画像命中规则
     */
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

    /**
     * 使用事实映射判断规则是否命中。
     *
     * @param rule 规则组
     * @param facts 字段到事实值的映射
     * @return true 表示事实满足规则
     */
    private boolean matchesFacts(RuleGroup rule, Map<String, Object> facts) {
        return evaluateGroup(rule, condition -> compare(facts.get(condition.field()), condition.op(), condition.value()));
    }

    /**
     * 递归计算规则组逻辑。
     *
     * @param group 当前规则组
     * @param predicate 条件求值函数
     * @return true 表示当前规则组命中
     */
    private boolean evaluateGroup(RuleGroup group, Predicate<RuleCondition> predicate) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (group.conditions().isEmpty() && group.groups().isEmpty()) {
            return true;
        }
        boolean or = "OR".equalsIgnoreCase(group.logic());
        boolean result = !or;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (RuleCondition condition : group.conditions()) {
            boolean current = predicate.test(condition);
            result = or ? result || current : result && current;
        }
        for (RuleGroup child : group.groups()) {
            boolean current = evaluateGroup(child, predicate);
            result = or ? result || current : result && current;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 比较单个事实值和规则期望值。
     *
     * @param actual 实际事实值
     * @param op 操作符
     * @param expected 期望值
     * @return true 表示比较成立
     */
    private boolean compare(Object actual, String op, Object expected) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (actual == null) {
            return false;
        }
        String normalizedOp = op == null ? "" : op.trim();
        if (actual instanceof Collection<?> values) {
            return compareCollection(values, normalizedOp, expected);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return switch (normalizedOp) {
            case "=" -> Objects.equals(stringValue(actual), stringValue(expected));
            case "!=" -> !Objects.equals(stringValue(actual), stringValue(expected));
            case ">" -> compareNumbers(actual, expected, comparison -> comparison > 0);
            case ">=" -> compareNumbers(actual, expected, comparison -> comparison >= 0);
            case "<" -> compareNumbers(actual, expected, comparison -> comparison < 0);
            case "<=" -> compareNumbers(actual, expected, comparison -> comparison <= 0);
            case "IN", "in" -> expected instanceof List<?> list
                    // 遍历候选数据并按业务规则筛选、转换或聚合。
                    && list.stream().map(this::stringValue).anyMatch(item -> item.equals(stringValue(actual)));
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    /**
     * 对多值事实执行规则比较。
     *
     * @param actualValues 实际多值集合
     * @param op 操作符
     * @param expected 期望值
     * @return true 表示集合中有值或全部值满足操作符语义
     */
    private boolean compareCollection(Collection<?> actualValues, String op, Object expected) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<?> values = actualValues.stream()
                .filter(Objects::nonNull)
                .toList();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values.isEmpty()) {
            return false;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 判断实际值是否包含在期望列表中。
     *
     * @param actual 实际值
     * @param expected 期望列表
     * @return true 表示期望列表包含实际值
     */
    private boolean matchesIn(Object actual, Object expected) {
        return expected instanceof List<?> list
                && list.stream().map(this::stringValue).anyMatch(item -> item.equals(stringValue(actual)));
    }

    /**
     * 按数值规则比较实际值和期望值。
     *
     * @param actual 实际值
     * @param expected 期望值
     * @param predicate BigDecimal 比较结果谓词
     * @return true 表示数值比较成立
     */
    private boolean compareNumbers(Object actual, Object expected, IntPredicate predicate) {
        BigDecimal actualNumber = numberOrNull(actual);
        if (actualNumber == null) {
            return false;
        }
        return predicate.test(actualNumber.compareTo(number(expected)));
    }

    /**
     * 向事实映射中追加多值字段。
     *
     * @param facts 事实映射
     * @param field 字段名
     * @param value 待追加值
     */
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

    /**
     * 解析人群规则 JSON。
     *
     * @param ruleJson 规则 JSON
     * @return 规则组，空规则返回默认 AND 空组
     */
    private RuleGroup parseRule(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) {
            return new RuleGroup("AND", List.of(), List.of());
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(ruleJson, new TypeReference<>() {});
            return toGroup(raw);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid audience ruleJson", e);
        }
    }

    /**
     * 将原始 Map 转换为规则组。
     *
     * @param raw 原始规则 Map
     * @return 规则组对象
     */
    @SuppressWarnings("unchecked")
    private RuleGroup toGroup(Map<String, Object> raw) {
        // 准备本次处理所需的上下文和中间变量。
        String logic = String.valueOf(raw.getOrDefault("logic", "AND")).toUpperCase(Locale.ROOT);
        List<RuleCondition> conditions = ((List<Object>) raw.getOrDefault("conditions", List.of()))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RuleGroup(logic, conditions, groups);
    }

    /**
     * 解析画像属性 JSON。
     *
     * @param json 属性 JSON
     * @return 属性映射，解析失败时返回空 Map
     */
    private Map<String, Object> parseProperties(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 将规则期望值解析为必需的数字。
     *
     * @param value 原始值
     * @return BigDecimal 数字值
     */
    private BigDecimal number(Object value) {
        try {
            return new BigDecimal(stringValue(value));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Audience condition value must be numeric: " + value);
        }
    }

    /**
     * 尝试将事实值解析为数字。
     *
     * @param value 原始值
     * @return BigDecimal 数字值，解析失败时返回 null
     */
    private BigDecimal numberOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(stringValue(value));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 归一化 CDP 来源类型。
     *
     * @param dataSourceType 原始来源类型
     * @return 大写来源类型，空值返回空字符串
     */
    private String normalizeSourceType(String dataSourceType) {
        return dataSourceType == null ? "" : dataSourceType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 归一化身份类型。
     *
     * @param identityType 原始身份类型
     * @return 大写身份类型，空值返回空字符串
     */
    private String normalizeIdentityType(String identityType) {
        return identityType == null ? "" : identityType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据属性值推断规则字段类型。
     *
     * @param value 属性值
     * @return NUMBER、BOOLEAN 或 STRING
     */
    private String inferValueType(Object value) {
        if (value instanceof Number) {
            return "NUMBER";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        return "STRING";
    }

    /**
     * 空白文本时返回兜底值。
     *
     * @param value 原始文本
     * @param fallback 兜底文本
     * @return 非空文本
     */
    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 将对象转换为字符串。
     *
     * @param value 原始对象
     * @return 字符串值，null 保持为 null
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * CDP 人群规则组。
     *
     * @param logic 组内逻辑，AND 或 OR
     * @param conditions 条件列表
     * @param groups 子规则组列表
     */
    private record RuleGroup(String logic, List<RuleCondition> conditions, List<RuleGroup> groups) {}

    /**
     * CDP 人群规则条件。
     *
     * @param field 字段名
     * @param op 操作符
     * @param value 期望值
     */
    private record RuleCondition(String field, String op, Object value) {}
}
