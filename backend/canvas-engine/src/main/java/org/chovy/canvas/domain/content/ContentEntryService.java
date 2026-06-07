package org.chovy.canvas.domain.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryDO;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryVersionDO;
import org.chovy.canvas.dal.mapper.MarketingContentEntryMapper;
import org.chovy.canvas.dal.mapper.MarketingContentEntryVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContentEntryService {

    private final MarketingContentEntryMapper entryMapper;
    private final MarketingContentEntryVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public ContentEntryService(MarketingContentEntryMapper entryMapper,
                               MarketingContentEntryVersionMapper versionMapper,
                               ObjectMapper objectMapper) {
        this.entryMapper = entryMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public List<EntryView> list(TenantContext tenant, String keyword, String contentType, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentEntryDO> query = new LambdaQueryWrapper<MarketingContentEntryDO>()
                .eq(MarketingContentEntryDO::getTenantId, tenantId)
                .orderByDesc(MarketingContentEntryDO::getUpdatedAt)
                .orderByDesc(MarketingContentEntryDO::getId);
        if (MarketingContentSupport.hasText(keyword)) {
            String pattern = "%" + keyword.trim() + "%";
            query.and(w -> w.like(MarketingContentEntryDO::getEntryKey, pattern)
                    .or()
                    .like(MarketingContentEntryDO::getTitle, pattern));
        }
        if (MarketingContentSupport.hasText(contentType)) {
            query.eq(MarketingContentEntryDO::getContentType, contentType.trim().toUpperCase());
        }
        if (MarketingContentSupport.hasText(status)) {
            query.eq(MarketingContentEntryDO::getStatus, status.trim().toUpperCase());
        }
        return entryMapper.selectList(query).stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public EntryView saveDraft(TenantContext tenant, EntryCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String entryKey = MarketingContentSupport.normalizeKey(command.entryKey(), "entryKey");
        String bodyJson = MarketingContentSupport.normalizeJsonObject(objectMapper, command.bodyJson(), "{}", "bodyJson");
        String seoJson = MarketingContentSupport.normalizeJsonObject(objectMapper, command.seoJson(), "{}", "seoJson");
        String assetRefsJson = MarketingContentSupport.normalizeJsonArray(objectMapper, command.assetRefsJson(), "[]", "assetRefsJson");
        String actor = MarketingContentSupport.operator(tenant, command.createdBy());

        MarketingContentEntryDO row = find(tenantId, entryKey);
        boolean insert = row == null;
        if (insert) {
            row = new MarketingContentEntryDO();
            row.setTenantId(tenantId);
            row.setEntryKey(entryKey);
            row.setCreatedBy(actor);
        }
        row.setContentType(MarketingContentSupport.requireText(command.contentType(), "contentType").toUpperCase());
        row.setTitle(MarketingContentSupport.requireText(command.title(), "entry title"));
        row.setSlug(MarketingContentSupport.normalizeSlug(command.slug()));
        row.setLocale(MarketingContentSupport.trimToNull(command.locale()));
        row.setSummary(MarketingContentSupport.trimToNull(command.summary()));
        row.setBodyJson(bodyJson);
        row.setSeoJson(seoJson);
        row.setAssetRefsJson(assetRefsJson);
        row.setStatus("DRAFT");
        row.setUpdatedBy(actor);
        row.setPublishedAt(null);
        if (insert) {
            entryMapper.insert(row);
        } else {
            entryMapper.updateById(row);
        }
        writeVersion(row, actor);
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public EntryView publish(TenantContext tenant, String entryKey, EntryStatusCommand command) {
        return transition(tenant, entryKey, command, "PUBLISHED");
    }

    @Transactional(rollbackFor = Exception.class)
    public EntryView archive(TenantContext tenant, String entryKey, EntryStatusCommand command) {
        return transition(tenant, entryKey, command, "ARCHIVED");
    }

    private EntryView transition(TenantContext tenant, String entryKey, EntryStatusCommand command, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String actor = MarketingContentSupport.operator(tenant, command == null ? null : command.actor());
        MarketingContentEntryDO row = require(tenantId, entryKey);
        row.setStatus(status);
        row.setUpdatedBy(actor);
        row.setPublishedAt("PUBLISHED".equals(status) ? LocalDateTime.now() : row.getPublishedAt());
        entryMapper.updateById(row);
        writeVersion(row, actor);
        return toView(row);
    }

    private MarketingContentEntryDO require(Long tenantId, String entryKey) {
        MarketingContentEntryDO row = find(tenantId, MarketingContentSupport.normalizeKey(entryKey, "entryKey"));
        if (row == null) {
            throw new IllegalArgumentException("content entry not found: " + entryKey);
        }
        return row;
    }

    private MarketingContentEntryDO find(Long tenantId, String entryKey) {
        return entryMapper.selectOne(new LambdaQueryWrapper<MarketingContentEntryDO>()
                .eq(MarketingContentEntryDO::getTenantId, tenantId)
                .eq(MarketingContentEntryDO::getEntryKey, entryKey)
                .last("LIMIT 1"));
    }

    private void writeVersion(MarketingContentEntryDO row, String actor) {
        MarketingContentEntryVersionDO version = new MarketingContentEntryVersionDO();
        version.setTenantId(row.getTenantId());
        version.setEntryKey(row.getEntryKey());
        version.setVersionNo(nextVersion(row.getTenantId(), row.getEntryKey()));
        version.setContentType(row.getContentType());
        version.setTitle(row.getTitle());
        version.setSlug(row.getSlug());
        version.setLocale(row.getLocale());
        version.setSummary(row.getSummary());
        version.setBodyJson(row.getBodyJson());
        version.setSeoJson(row.getSeoJson());
        version.setAssetRefsJson(row.getAssetRefsJson());
        version.setStatus(row.getStatus());
        version.setCreatedBy(actor);
        versionMapper.insert(version);
    }

    private int nextVersion(Long tenantId, String entryKey) {
        MarketingContentEntryVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<MarketingContentEntryVersionDO>()
                .eq(MarketingContentEntryVersionDO::getTenantId, tenantId)
                .eq(MarketingContentEntryVersionDO::getEntryKey, entryKey)
                .orderByDesc(MarketingContentEntryVersionDO::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    private EntryView toView(MarketingContentEntryDO row) {
        return new EntryView(
                row.getEntryKey(),
                row.getContentType(),
                row.getTitle(),
                row.getSlug(),
                row.getLocale(),
                row.getStatus(),
                row.getBodyJson(),
                row.getAssetRefsJson());
    }

    public record EntryCommand(
            String entryKey,
            String contentType,
            String title,
            String slug,
            String locale,
            String summary,
            String bodyJson,
            String seoJson,
            String assetRefsJson,
            String createdBy) {
    }

    public record EntryStatusCommand(String actor) {
    }

    public record EntryView(
            String entryKey,
            String contentType,
            String title,
            String slug,
            String locale,
            String status,
            String bodyJson,
            String assetRefsJson) {
    }
}
