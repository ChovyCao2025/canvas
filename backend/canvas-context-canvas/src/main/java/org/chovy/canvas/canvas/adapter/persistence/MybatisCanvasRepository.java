package org.chovy.canvas.canvas.adapter.persistence;

import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasListItem;
import org.chovy.canvas.canvas.domain.CanvasListQuery;
import org.chovy.canvas.canvas.domain.CanvasPage;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.springframework.stereotype.Repository;

/**
 * 封装MybatisCanvasRepository相关的业务逻辑。
 */
@Repository
public class MybatisCanvasRepository implements CanvasRepository {

    /**
     * 保存映射器。
     */
    private final CanvasMapper mapper;

    /**
     * 创建当前对象实例。
     */
    public MybatisCanvasRepository(CanvasMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存。
     */
    @Override
    public Canvas save(Canvas canvas) {
        CanvasDO row = CanvasPersistenceMapper.toRow(canvas);
        if (row.getId() == null) {
            int inserted = mapper.insert(row);
            if (inserted <= 0) {
                throw new IllegalStateException("Canvas insert affected 0 rows");
            }
        } else {
            int updated = mapper.updateById(row);
            if (updated <= 0) {
                throw new IllegalStateException("Canvas update affected 0 rows: " + row.getId());
            }
        }
        return CanvasPersistenceMapper.toDomain(row);
    }

    /**
     * 查询by标识。
     */
    @Override
    public Optional<Canvas> findById(Long canvasId) {
        return Optional.ofNullable(CanvasPersistenceMapper.toDomain(mapper.selectById(canvasId)));
    }

    /**
     * 分页查询画布列表。
     */
    @Override
    public CanvasPage<CanvasListItem> list(CanvasListQuery query) {
        CanvasListQuery q = query == null ? new CanvasListQuery(1, 20, null, null, null, null, null, null) : query;
        Long total = mapper.selectCount(listWrapper(q, false));
        IPage<CanvasDO> page = mapper.selectPage(new Page<>(q.page(), q.size(), false), listWrapper(q, true));
        return CanvasPage.of(total, page.getRecords().stream()
                .map(MybatisCanvasRepository::toListItem)
                .toList());
    }

    private static LambdaQueryWrapper<CanvasDO> listWrapper(CanvasListQuery q, boolean ordered) {
        LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<CanvasDO>()
                .eq(q.tenantId() != null, CanvasDO::getTenantId, q.tenantId())
                .eq(q.status() != null, CanvasDO::getStatus, q.status())
                .ne(q.status() == null, CanvasDO::getStatus, 3)
                .like(q.name() != null, CanvasDO::getName, q.name())
                .eq(q.projectKey() != null, CanvasDO::getProjectKey, q.projectKey())
                .eq(q.folderKey() != null, CanvasDO::getFolderKey, q.folderKey())
                .inSql(q.projectId() != null, CanvasDO::getId,
                        "SELECT canvas_id FROM canvas_project_folder WHERE project_id = " + q.projectId());
        return ordered ? wrapper.orderByDesc(CanvasDO::getCreatedAt) : wrapper;
    }

    private static CanvasListItem toListItem(CanvasDO row) {
        return new CanvasListItem(
                row.getId(),
                row.getTenantId(),
                row.getName(),
                row.getDescription(),
                row.getStatus(),
                row.getPublishedVersionId(),
                row.getCanaryVersionId(),
                row.getCanaryPercent(),
                row.getCreatedBy(),
                row.getIsExample(),
                row.getSourceTemplateKey(),
                row.getProjectKey(),
                row.getProjectName(),
                row.getFolderKey(),
                row.getFolderName(),
                format(row.getCreatedAt()),
                format(row.getUpdatedAt()),
                row.getTriggerType(),
                row.getCronExpression(),
                row.getEditVersion(),
                format(row.getValidStart()),
                format(row.getValidEnd()),
                row.getMaxTotalExecutions(),
                row.getPerUserDailyLimit(),
                row.getPerUserTotalLimit(),
                row.getCooldownSeconds(),
                row.getControlGroupPercent(),
                row.getControlGroupSalt(),
                row.getConversionEventCode(),
                row.getAttributionWindowDays());
    }

    private static String format(java.time.LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
