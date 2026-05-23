package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.domain.cdp.CdpUserTag;
import org.chovy.canvas.domain.cdp.CdpUserTagMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TagDefinitionService {

    private static final Pattern TAG_CODE_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]{1,63}");

    private final TagDefinitionMapper tagDefinitionMapper;
    private final TagValueDefinitionMapper tagValueDefinitionMapper;
    private final CdpUserTagMapper cdpUserTagMapper;

    public PageResult<TagDefinition> page(int page, int size, String tagType, Integer enabled) {
        Page<TagDefinition> result = tagDefinitionMapper.selectPage(
                new Page<>(Math.max(1, page), Math.max(1, size)),
                query(tagType, enabled));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public List<TagDefinition> list(String tagType, Integer enabled) {
        return tagDefinitionMapper.selectList(query(tagType, enabled));
    }

    public TagDefinition create(TagDefinition body) {
        validateAndNormalize(body);
        applyDefaults(body);
        tagDefinitionMapper.insert(body);
        return body;
    }

    public void update(Long id, TagDefinition body) {
        validateAndNormalize(body);
        applyDefaults(body);
        body.setId(id);
        tagDefinitionMapper.updateById(body);
    }

    public void delete(Long id) {
        TagDefinition existing = tagDefinitionMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("tag definition not found: " + id);
        }
        Long count = cdpUserTagMapper.selectCount(new LambdaQueryWrapper<CdpUserTag>()
                .eq(CdpUserTag::getTagCode, existing.getTagCode()));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("tag definition is in use: " + existing.getTagCode());
        }
        tagDefinitionMapper.deleteById(id);
    }

    public List<TagValueDefinition> listValues(String tagCode, Integer enabled) {
        String normalizedTagCode = normalizeTagCode(tagCode);
        return tagValueDefinitionMapper.selectList(new LambdaQueryWrapper<TagValueDefinition>()
                .eq(TagValueDefinition::getTagCode, normalizedTagCode)
                .eq(enabled != null, TagValueDefinition::getEnabled, enabled)
                .orderByAsc(TagValueDefinition::getSortOrder)
                .orderByAsc(TagValueDefinition::getId));
    }

    public TagValueDefinition createValue(String tagCode, TagValueDefinition body) {
        if (body == null) {
            throw new IllegalArgumentException("tag value body is required");
        }
        TagDefinition definition = requireTag(tagCode);
        validateAndNormalizeValueBody(body, definition, normalizeTagCode(tagCode), false);
        applyValueDefaults(body);
        tagValueDefinitionMapper.insert(body);
        return body;
    }

    public void updateValue(Long id, TagValueDefinition body) {
        if (body == null) {
            throw new IllegalArgumentException("tag value body is required");
        }
        TagValueDefinition existingValue = tagValueDefinitionMapper.selectById(id);
        if (existingValue == null) {
            throw new IllegalArgumentException("tag value definition not found: " + id);
        }
        TagDefinition definition = requireTag(existingValue.getTagCode());
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

    public void deleteValue(Long id) {
        TagValueDefinition existing = tagValueDefinitionMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("tag value definition not found: " + id);
        }
        Long count = cdpUserTagMapper.selectCount(new LambdaQueryWrapper<CdpUserTag>()
                .eq(CdpUserTag::getTagCode, existing.getTagCode())
                .eq(CdpUserTag::getTagValue, existing.getValue()));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("tag value definition is in use: " + existing.getTagCode() + "=" + existing.getValue());
        }
        tagValueDefinitionMapper.deleteById(id);
    }

    public TagDefinition requireEnabledTagAndValidateValue(String tagCode, String tagValue) {
        String normalizedTagCode = normalizeTagCode(tagCode);
        TagDefinition definition = tagDefinitionMapper.selectOne(new LambdaQueryWrapper<TagDefinition>()
                .eq(TagDefinition::getTagCode, normalizedTagCode));
        if (definition == null || definition.getEnabled() == null || definition.getEnabled() != 1) {
            throw new IllegalArgumentException("tag definition is not enabled: " + normalizedTagCode);
        }
        validateTagValue(definition.getValueType(), tagValue);
        return definition;
    }

    public void ensureValue(String tagCode, String tagValue, String source) {
        TagDefinition definition = requireEnabledTagAndValidateValue(tagCode, tagValue);
        String normalizedTagCode = definition.getTagCode();
        String normalizedValue = normalizeValue(tagValue);
        TagValueDefinition existing = tagValueDefinitionMapper.selectOne(new LambdaQueryWrapper<TagValueDefinition>()
                .eq(TagValueDefinition::getTagCode, normalizedTagCode)
                .eq(TagValueDefinition::getValue, normalizedValue));
        if (existing != null) {
            return;
        }
        TagValueDefinition value = new TagValueDefinition();
        value.setTagCode(normalizedTagCode);
        value.setValue(normalizedValue);
        value.setLabel(normalizedValue);
        value.setEnabled(1);
        value.setSortOrder(0);
        value.setSource(normalizeSource(source));
        tagValueDefinitionMapper.insert(value);
    }

    private TagDefinition requireTag(String tagCode) {
        String normalizedTagCode = normalizeTagCode(tagCode);
        TagDefinition definition = tagDefinitionMapper.selectOne(new LambdaQueryWrapper<TagDefinition>()
                .eq(TagDefinition::getTagCode, normalizedTagCode));
        if (definition == null) {
            throw new IllegalArgumentException("tag definition not found: " + normalizedTagCode);
        }
        return definition;
    }

    private LambdaQueryWrapper<TagDefinition> query(String tagType, Integer enabled) {
        return new LambdaQueryWrapper<TagDefinition>()
                .eq(tagType != null && !tagType.isBlank(), TagDefinition::getTagType, normalizeTagType(tagType))
                .eq(enabled != null, TagDefinition::getEnabled, enabled)
                .orderByAsc(TagDefinition::getId);
    }

    private static void validateAndNormalize(TagDefinition body) {
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

    private static void applyDefaults(TagDefinition body) {
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

    private static void validateAndNormalizeValueBody(
            TagValueDefinition body, TagDefinition definition, String expectedTagCode, boolean allowMissingValue) {
        String normalizedTagCode = normalizeTagCode(body.getTagCode());
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

    private static void applyValueDefaults(TagValueDefinition body) {
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

    private static void validateTagValue(String valueType, String tagValue) {
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
                return;
            default:
                throw new IllegalArgumentException("unsupported valueType: " + normalizedValueType);
        }
    }

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

    private static String normalizeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("tagValue is required");
        }
        return value.trim();
    }

    private static String normalizeSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "MANUAL";
        }
        return source.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
