package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 画布主表 Mapper（表：canvas）。
 *
 * <p>除基础 CRUD 外，还承载编辑版本号 CAS 更新能力。
 */
@Mapper
public interface CanvasMapper extends BaseMapper<Canvas> {

    /**
     * 乐观锁保存：edit_version CAS。
     * SQL 在 CanvasMapper.xml 中定义。
     */
    int updateEditVersion(@Param("id") Long id,
                          @Param("oldVersion") int oldVersion,
                          @Param("newVersion") int newVersion,
                          @Param("name") String name,
                          @Param("description") String description);

    // 画布发布/下线/归档等复合操作由 Service 编排多个 Mapper 完成。
    // 草稿图内容写入 canvas_version，不在该接口中处理。
}
