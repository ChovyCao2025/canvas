package org.chovy.canvas.domain.risk.governance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.RiskListDO;
import org.chovy.canvas.dal.dataobject.RiskListEntryDO;
import org.chovy.canvas.dal.mapper.RiskListEntryMapper;
import org.chovy.canvas.dal.mapper.RiskListMapper;
import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.domain.risk.governance.RiskListService.StateStore;
import org.chovy.canvas.domain.risk.runtime.RiskListType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed 风控名单仓储，使名单元数据和条目可跨进程重启恢复。
 */
public class JdbcRiskListStore implements StateStore {

    private final RiskListMapper listMapper;
    private final RiskListEntryMapper entryMapper;
    private final Clock clock;

    /**
     * 创建 JDBC 名单仓储。
     */
    public JdbcRiskListStore(RiskListMapper listMapper,
                             RiskListEntryMapper entryMapper,
                             Clock clock) {
        this.listMapper = listMapper;
        this.entryMapper = entryMapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public Optional<RiskListView> findList(Long tenantId, String listKey) {
        return Optional.ofNullable(listMapper.selectOne(listQuery(tenantId, listKey))).map(this::toView);
    }

    @Override
    public List<RiskListView> listLists(Long tenantId) {
        return listMapper.selectList(new LambdaQueryWrapper<RiskListDO>()
                        .eq(RiskListDO::getTenantId, tenantId)
                        .orderByAsc(RiskListDO::getListKey))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public void saveList(RiskListView list) {
        LocalDateTime now = LocalDateTime.now(clock);
        RiskListDO existing = listMapper.selectOne(listQuery(list.tenantId(), list.listKey()));
        RiskListDO row = toRow(list, now);
        if (existing == null) {
            row.setCreatedAt(now);
            listMapper.insert(row);
        } else {
            row.setId(existing.getId());
            row.setCreatedAt(existing.getCreatedAt());
            listMapper.updateById(row);
        }
    }

    @Override
    public List<RiskListEntryView> entries(Long tenantId, String listKey) {
        return entryMapper.selectList(new LambdaQueryWrapper<RiskListEntryDO>()
                        .eq(RiskListEntryDO::getTenantId, tenantId)
                        .eq(RiskListEntryDO::getListKey, listKey))
                .stream()
                .map(this::toEntryView)
                .toList();
    }

    @Override
    public RiskListEntryView saveEntry(RiskListEntryView entry) {
        RiskListEntryDO row = toEntryRow(entry);
        entryMapper.insert(row);
        long id = row.getId() == null ? entry.id() : row.getId();
        return new RiskListEntryView(id, entry.tenantId(), entry.listKey(), entry.subjectHash(),
                entry.subjectMasked(), entry.reason(), entry.source(), entry.effectiveFrom(),
                entry.expiresAt(), entry.createdBy());
    }

    @Override
    public void removeEntry(Long tenantId, String listKey, long entryId) {
        entryMapper.delete(new LambdaQueryWrapper<RiskListEntryDO>()
                .eq(RiskListEntryDO::getTenantId, tenantId)
                .eq(RiskListEntryDO::getListKey, listKey)
                .eq(RiskListEntryDO::getId, entryId));
    }

    private LambdaQueryWrapper<RiskListDO> listQuery(Long tenantId, String listKey) {
        return new LambdaQueryWrapper<RiskListDO>()
                .eq(RiskListDO::getTenantId, tenantId)
                .eq(RiskListDO::getListKey, listKey);
    }

    private RiskListDO toRow(RiskListView view, LocalDateTime now) {
        RiskListDO row = new RiskListDO();
        row.setTenantId(view.tenantId());
        row.setListKey(view.listKey());
        row.setListType(view.listType().name());
        row.setSubjectType(view.subjectType().name());
        row.setStatus(view.status());
        row.setRequiresApproval(view.requiresApproval());
        row.setOwner(view.owner());
        row.setUpdatedAt(now);
        return row;
    }

    private RiskListView toView(RiskListDO row) {
        return new RiskListView(row.getTenantId(), row.getListKey(), RiskListType.valueOf(row.getListType()),
                RiskSubjectType.valueOf(row.getSubjectType()), row.getStatus(), row.getRequiresApproval(), row.getOwner());
    }

    private RiskListEntryDO toEntryRow(RiskListEntryView view) {
        RiskListEntryDO row = new RiskListEntryDO();
        row.setTenantId(view.tenantId());
        row.setListKey(view.listKey());
        row.setSubjectHash(view.subjectHash());
        row.setSubjectMasked(view.subjectMasked());
        row.setReason(view.reason());
        row.setSource(view.source());
        row.setEffectiveFrom(local(view.effectiveFrom() == null ? clock.instant() : view.effectiveFrom()));
        row.setExpiresAt(view.expiresAt() == null ? null : local(view.expiresAt()));
        row.setCreatedBy(view.createdBy());
        row.setCreatedAt(LocalDateTime.now(clock));
        return row;
    }

    private RiskListEntryView toEntryView(RiskListEntryDO row) {
        return new RiskListEntryView(row.getId(), row.getTenantId(), row.getListKey(), row.getSubjectHash(),
                row.getSubjectMasked(), row.getReason(), row.getSource(),
                instant(row.getEffectiveFrom()), instant(row.getExpiresAt()), row.getCreatedBy());
    }

    private LocalDateTime local(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
