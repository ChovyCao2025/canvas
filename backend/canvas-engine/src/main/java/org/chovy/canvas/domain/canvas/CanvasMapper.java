package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
