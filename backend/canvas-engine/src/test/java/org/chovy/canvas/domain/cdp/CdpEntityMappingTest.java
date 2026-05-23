package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CdpEntityMappingTest {

    @Test
    void mapsEntitiesToCdpTables() {
        assertThat(CdpUserProfile.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_profile");
        assertThat(CdpUserIdentity.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_identity");
        assertThat(CdpUserTag.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag");
        assertThat(CdpUserTagHistory.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag_history");
        assertThat(CdpTagOperation.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_tag_operation");
    }

    @Test
    void exposesCoreFields() {
        CdpUserTag tag = new CdpUserTag();
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
