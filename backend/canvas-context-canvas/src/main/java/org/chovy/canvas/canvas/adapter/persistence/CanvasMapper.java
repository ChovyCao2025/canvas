package org.chovy.canvas.canvas.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 定义CanvasMapper对外提供的能力契约。
 */
@Mapper
public interface CanvasMapper extends BaseMapper<CanvasDO> {

    /**
     * 更新画布编辑版本号。
     */
    int updateEditVersion(@Param("id") Long id,
                          @Param("oldVersion") int oldVersion,
                          @Param("newVersion") int newVersion,
                          @Param("name") String name,
                          @Param("description") String description);
}
