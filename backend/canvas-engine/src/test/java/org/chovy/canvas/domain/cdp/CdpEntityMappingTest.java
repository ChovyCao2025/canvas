package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.chovy.canvas.dal.dataobject.CdpTagOperationDO;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagHistoryDO;

/**
 * Cdp Entity Mapping 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CdpEntityMappingTest {

    @Test
    void mapsEntitiesToCdpTables() {
        assertThat(CdpUserProfileDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_profile");
        assertThat(CdpUserIdentityDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_identity");
        assertThat(CdpUserTagDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag");
        assertThat(CdpUserTagHistoryDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag_history");
        assertThat(CdpTagOperationDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_tag_operation");
    }

    @Test
    void exposesCoreFields() {
        CdpUserTagDO tag = new CdpUserTagDO();
        tag.setUserId("u1");
        tag.setTagCode("vip");
        tag.setTagValue("true");
        tag.setStatus("ACTIVE");

        assertThat(tag.getUserId()).isEqualTo("u1");
        assertThat(tag.getTagCode()).isEqualTo("vip");
        assertThat(tag.getTagValue()).isEqualTo("true");
        assertThat(tag.getStatus()).isEqualTo("ACTIVE");
    }
}
