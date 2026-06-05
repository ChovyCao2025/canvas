package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.query.CanvasListQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasProjectFilterTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                CanvasDO.class);
    }

    @Test
    void listAppliesProjectAndFolderFilters() {
        CanvasListQuery query = new CanvasListQuery();
        query.setProjectKey("growth");
        query.setFolderKey("new-user");

        LambdaQueryWrapper<CanvasDO> wrapper = CanvasListQuerySupport.build(query, true);

        String sql = wrapper.getTargetSql();
        assertThat(sql).contains("project_key", "folder_key");
    }
}
