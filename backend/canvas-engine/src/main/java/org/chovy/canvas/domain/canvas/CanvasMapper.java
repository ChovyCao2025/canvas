package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CanvasMapper extends BaseMapper<Canvas> {

    /** 乐观锁保存：edit_version CAS */
    @Update("UPDATE canvas SET name=#{name}, description=#{description}, " +
            "edit_version=#{newVersion}, updated_at=NOW() " +
            "WHERE id=#{id} AND edit_version=#{oldVersion}")
    int updateEditVersion(@Param("id") Long id,
                          @Param("oldVersion") int oldVersion,
                          @Param("newVersion") int newVersion,
                          @Param("name") String name,
                          @Param("description") String description);
}
