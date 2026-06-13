package org.chovy.canvas.canvas.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CanvasMapper extends BaseMapper<CanvasDO> {

    int updateEditVersion(@Param("id") Long id,
                          @Param("oldVersion") int oldVersion,
                          @Param("newVersion") int newVersion,
                          @Param("name") String name,
                          @Param("description") String description);
}
