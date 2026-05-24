package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.chovy.canvas.dal.dataobject.CdpTagOperationDO;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagHistoryDO;

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
