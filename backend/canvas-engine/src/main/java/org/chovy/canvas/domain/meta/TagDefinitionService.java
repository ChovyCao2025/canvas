package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.chovy.canvas.dal.dataobject.TagValueDefinitionDO;
import org.chovy.canvas.dal.mapper.TagValueDefinitionMapper;

/**
 * 标签定义 元数据领域服务。
 *
 * <p>负责事件、接口、标签、系统选项或实验分组等配置型数据的维护和查询。
 * <p>元数据会影响画布运行时行为，因此该层需要兼顾管理端易用性与执行链路缓存一致性。
 */
@Service
@RequiredArgsConstructor
public class TagDefinitionService {

    /** 标签编码格式：字母开头，后续允许字母、数字和下划线。 */
    private static final Pattern TAG_CODE_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]{1,63}");

    /** 标签定义 Mapper。 */
    private final TagDefinitionMapper tagDefinitionMapper;
    /** 标签值定义 Mapper。 */
    private final TagValueDefinitionMapper tagValueDefinitionMapper;
    /** CDP 用户标签 Mapper，用于判断标签定义是否仍被使用。 */
    private final CdpUserTagMapper cdpUserTagMapper;

    /** 分页查询数据。 */
    public PageResult<TagDefinitionDO> page(int page, int size, String tagType, Integer enabled) {
        Page<TagDefinitionDO> result = tagDefinitionMapper.selectPage(
                new Page<>(Math.max(1, page), Math.max(1, size)),
                query(tagType, enabled));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 按条件查询列表数据。 */
    public List<TagDefinitionDO> list(String tagType, Integer enabled) {
        return tagDefinitionMapper.selectList(query(tagType, enabled));
    }

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
    public TagDefinitionDO create(TagDefinitionDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        tagDefinitionMapper.insert(body);
        return body;
    }

    /** 更新已有记录，仅修改允许变更的字段。 */
    public void update(Long id, TagDefinitionDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        body.setId(id);
        tagDefinitionMapper.updateById(body);
    }

    /** 删除标签定义，若已有用户标签引用则阻止删除。 */
    public void delete(Long id) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        TagDefinitionDO existing = tagDefinitionMapper.selectById(id);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing == null) {
            throw new IllegalArgumentException("tag definition not found: " + id);
        }
        Long count = cdpUserTagMapper.selectCount(new LambdaQueryWrapper<CdpUserTagDO>()
                .eq(CdpUserTagDO::getTagCode, existing.getTagCode()));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("tag definition is in use: " + existing.getTagCode());
        }
        tagDefinitionMapper.deleteById(id);
    }

    /** 查询指定标签的可选值。 */
    public List<TagValueDefinitionDO> listValues(String tagCode, Integer enabled) {
        String normalizedTagCode = normalizeTagCode(tagCode);
        return tagValueDefinitionMapper.selectList(new LambdaQueryWrapper<TagValueDefinitionDO>()
                .eq(TagValueDefinitionDO::getTagCode, normalizedTagCode)
                .eq(enabled != null, TagValueDefinitionDO::getEnabled, enabled)
                .orderByAsc(TagValueDefinitionDO::getSortOrder)
                .orderByAsc(TagValueDefinitionDO::getId));
    }

    /** 创建标签可选值。 */
    public TagValueDefinitionDO createValue(String tagCode, TagValueDefinitionDO body) {
        if (body == null) {
            throw new IllegalArgumentException("tag value body is required");
        }
        TagDefinitionDO definition = requireTag(tagCode);
        validateAndNormalizeValueBody(body, definition, normalizeTagCode(tagCode), false);
        applyValueDefaults(body);
        tagValueDefinitionMapper.insert(body);
        return body;
    }

    /** 更新标签可选值。 */
    public void updateValue(Long id, TagValueDefinitionDO body) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (body == null) {
            throw new IllegalArgumentException("tag value body is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        TagValueDefinitionDO existingValue = tagValueDefinitionMapper.selectById(id);
        if (existingValue == null) {
            throw new IllegalArgumentException("tag value definition not found: " + id);
        }
        TagDefinitionDO definition = requireTag(existingValue.getTagCode());
        if (body.getTagCode() == null || body.getTagCode().isBlank()) {
            body.setTagCode(existingValue.getTagCode());
        }
        if (body.getValue() == null || body.getValue().isBlank()) {
            body.setValue(existingValue.getValue());
        }
        if (body.getLabel() == null || body.getLabel().isBlank()) {
            body.setLabel(existingValue.getLabel());
        }
        validateAndNormalizeValueBody(body, definition, existingValue.getTagCode(), true);
        applyValueDefaults(body);
        body.setId(id);
        tagValueDefinitionMapper.updateById(body);
    }

    /** 删除标签可选值。 */
    public void deleteValue(Long id) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        TagValueDefinitionDO existing = tagValueDefinitionMapper.selectById(id);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing == null) {
            throw new IllegalArgumentException("tag value definition not found: " + id);
        }
        Long count = cdpUserTagMapper.selectCount(new LambdaQueryWrapper<CdpUserTagDO>()
                .eq(CdpUserTagDO::getTagCode, existing.getTagCode())
                .eq(CdpUserTagDO::getTagValue, existing.getValue()));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("tag value definition is in use: " + existing.getTagCode() + "=" + existing.getValue());
        }
        tagValueDefinitionMapper.deleteById(id);
    }

    /** 校验标签启用状态和标签值合法性，失败时抛出异常。 */
    public TagDefinitionDO requireEnabledTagAndValidateValue(String tagCode, String tagValue) {
        String normalizedTagCode = normalizeTagCode(tagCode);
        TagDefinitionDO definition = tagDefinitionMapper.selectOne(new LambdaQueryWrapper<TagDefinitionDO>()
                .eq(TagDefinitionDO::getTagCode, normalizedTagCode));
        if (definition == null || definition.getEnabled() == null || definition.getEnabled() != 1) {
            throw new IllegalArgumentException("tag definition is not enabled: " + normalizedTagCode);
        }
        validateTagValue(definition.getValueType(), tagValue);
        return definition;
    }

    /** 确保标签值存在，不存在时按来源补充。 */
    public void ensureValue(String tagCode, String tagValue, String source) {
        TagDefinitionDO definition = requireEnabledTagAndValidateValue(tagCode, tagValue);
        String normalizedTagCode = definition.getTagCode();
        String normalizedValue = normalizeValue(tagValue);
        TagValueDefinitionDO existing = tagValueDefinitionMapper.selectOne(new LambdaQueryWrapper<TagValueDefinitionDO>()
                .eq(TagValueDefinitionDO::getTagCode, normalizedTagCode)
                .eq(TagValueDefinitionDO::getValue, normalizedValue));
        if (existing != null) {
            return;
        }
        TagValueDefinitionDO value = new TagValueDefinitionDO();
        value.setTagCode(normalizedTagCode);
        value.setValue(normalizedValue);
        value.setLabel(normalizedValue);
        value.setEnabled(1);
        value.setSortOrder(0);
        value.setSource(normalizeSource(source));
        tagValueDefinitionMapper.insert(value);
    }

    /** 查询标签定义，不存在时抛出异常，供标签值维护流程复用。 */
    private TagDefinitionDO requireTag(String tagCode) {
        String normalizedTagCode = normalizeTagCode(tagCode);
        TagDefinitionDO definition = tagDefinitionMapper.selectOne(new LambdaQueryWrapper<TagDefinitionDO>()
                .eq(TagDefinitionDO::getTagCode, normalizedTagCode));
        if (definition == null) {
            throw new IllegalArgumentException("tag definition not found: " + normalizedTagCode);
        }
        return definition;
    }

    /** 组装标签定义查询条件，统一处理类型、启用状态和排序。 */
    private LambdaQueryWrapper<TagDefinitionDO> query(String tagType, Integer enabled) {
        return new LambdaQueryWrapper<TagDefinitionDO>()
                .eq(tagType != null && !tagType.isBlank(), TagDefinitionDO::getTagType, normalizeTagType(tagType))
                .eq(enabled != null, TagDefinitionDO::getEnabled, enabled)
                .orderByAsc(TagDefinitionDO::getId);
    }

    /** 校验标签定义必填字段并规范化编码、类型和值类型。 */
    private static void validateAndNormalize(TagDefinitionDO body) {
        if (body == null) {
            throw new IllegalArgumentException("tag definition body is required");
        }
        if (body.getName() == null || body.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        String normalizedTagCode = normalizeTagCode(body.getTagCode());
        String normalizedTagType = normalizeTagType(body.getTagType());
        String normalizedValueType = normalizeValueType(body.getValueType());
        body.setName(body.getName().trim());
        body.setTagCode(normalizedTagCode);
        body.setTagType(normalizedTagType);
        body.setValueType(normalizedValueType);
    }

    /** 为标签定义写入启用状态、写入策略和描述类字段默认值。 */
    private static void applyDefaults(TagDefinitionDO body) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
        if (body.getValueType() == null) {
            body.setValueType("STRING");
        }
        if (body.getManualEnabled() == null) {
            body.setManualEnabled(1);
        }
        if (body.getWritePolicy() == null || body.getWritePolicy().trim().isEmpty()) {
            body.setWritePolicy("UPSERT");
        } else {
            String normalized = body.getWritePolicy().trim().toUpperCase(Locale.ROOT);
            if (!"UPSERT".equals(normalized) && !"APPEND".equals(normalized)) {
                throw new IllegalArgumentException("invalid writePolicy: " + normalized);
            }
            body.setWritePolicy(normalized);
        }
        body.setCategory(trimToNull(body.getCategory()));
        body.setOwner(trimToNull(body.getOwner()));
        body.setDescription(trimToNull(body.getDescription()));
    }

    /** 校验标签可选值所属标签和值内容，并规范化展示文案。 */
    private static void validateAndNormalizeValueBody(
            TagValueDefinitionDO body, TagDefinitionDO definition, String expectedTagCode, boolean allowMissingValue) {
        // 准备本次处理所需的上下文和中间变量。
        String normalizedTagCode = normalizeTagCode(body.getTagCode());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!expectedTagCode.equals(normalizedTagCode)) {
            throw new IllegalArgumentException("tagCode does not match existing definition");
        }
        if (!allowMissingValue || body.getValue() != null) {
            validateTagValue(definition.getValueType(), body.getValue());
            body.setValue(normalizeValue(body.getValue()));
        }
        if (body.getValue() == null && !allowMissingValue) {
            throw new IllegalArgumentException("value is required");
        }
        body.setTagCode(normalizedTagCode);
        if (body.getLabel() == null || body.getLabel().trim().isEmpty()) {
            body.setLabel(body.getValue());
        } else {
            body.setLabel(body.getLabel().trim());
        }
        if (body.getDescription() != null) {
            body.setDescription(body.getDescription().trim());
        }
    }

    /** 为标签可选值写入排序、启用状态和来源默认值。 */
    private static void applyValueDefaults(TagValueDefinitionDO body) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (body.getSortOrder() == null) {
            body.setSortOrder(0);
        }
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
        if (body.getSource() == null || body.getSource().trim().isEmpty()) {
            body.setSource("MANUAL");
        } else {
            body.setSource(body.getSource().trim().toUpperCase(Locale.ROOT));
        }
    }

    /** 按标签值类型校验候选值，确保 NUMBER、BOOLEAN 等类型可被执行链路消费。 */
    private static void validateTagValue(String valueType, String tagValue) {
        // 准备本次处理所需的上下文和中间变量。
        String normalizedValue = normalizeValue(tagValue);
        String normalizedValueType = normalizeValueType(valueType);
        switch (normalizedValueType) {
            case "STRING":
                return;
            case "NUMBER":
                try {
                    new BigDecimal(normalizedValue);
                    return;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("tag value must match NUMBER: " + normalizedValue);
                }
            case "BOOLEAN":
                String boolValue = normalizedValue.toLowerCase(Locale.ROOT);
                if (!"true".equals(boolValue) && !"false".equals(boolValue)) {
                    throw new IllegalArgumentException("tag value must match BOOLEAN: " + normalizedValue);
                }
                return;
            case "JSON":
                // 汇总前面计算出的状态和明细，返回给调用方。
                return;
            default:
                throw new IllegalArgumentException("unsupported valueType: " + normalizedValueType);
        }
    }

    /** 校验并返回标准标签编码。 */
    private static String normalizeTagCode(String tagCode) {
        if (tagCode == null) {
            throw new IllegalArgumentException("tagCode is required");
        }
        String normalized = tagCode.trim();
        if (!TAG_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid tagCode: " + normalized);
        }
        return normalized;
    }

    /** 校验并返回标准标签类型，当前仅支持离线和实时标签。 */
    private static String normalizeTagType(String tagType) {
        if (tagType == null || tagType.trim().isEmpty()) {
            throw new IllegalArgumentException("tagType is required");
        }
        String normalized = tagType.trim().toLowerCase(Locale.ROOT);
        if (!"offline".equals(normalized) && !"realtime".equals(normalized)) {
            throw new IllegalArgumentException("invalid tagType: " + normalized);
        }
        return normalized;
    }

    /** 将标签值类型统一为系统支持的大写枚举值。 */
    private static String normalizeValueType(String valueType) {
        if (valueType == null || valueType.trim().isEmpty()) {
            return "STRING";
        }
        String normalized = valueType.trim().toUpperCase(Locale.ROOT);
        if (!"STRING".equals(normalized)
                && !"NUMBER".equals(normalized)
                && !"BOOLEAN".equals(normalized)
                && !"JSON".equals(normalized)) {
            throw new IllegalArgumentException("invalid valueType: " + normalized);
        }
        return normalized;
    }

    /** 校验标签值文本并返回去除首尾空白后的值。 */
    private static String normalizeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("tagValue is required");
        }
        return value.trim();
    }

    /** 将标签值来源统一为大写来源标识，缺省使用 MANUAL。 */
    private static String normalizeSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "MANUAL";
        }
        return source.trim().toUpperCase(Locale.ROOT);
    }

    /** 将可选文本字段去除首尾空白，空串统一保存为 null。 */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
