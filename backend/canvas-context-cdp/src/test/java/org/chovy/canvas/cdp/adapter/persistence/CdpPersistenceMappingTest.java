package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.chovy.canvas.cdp.domain.AudienceSnapshot;
import org.chovy.canvas.cdp.domain.AudienceSnapshotMode;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpTagDefinition;
import org.chovy.canvas.cdp.domain.CdpUserTag;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CdpPersistenceMappingTest {

    private final CdpPersistenceConverter converter = new CdpPersistenceConverter();

    @Test
    void cdpDoClassesOwnLegacyTablesInsideCdpPersistenceAdapter() {
        assertThat(CdpUserProfileDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_profile");
        assertThat(CdpUserIdentityDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_identity");
        assertThat(TagDefinitionDO.class.getAnnotation(TableName.class).value()).isEqualTo("tag_definition");
        assertThat(CdpUserTagDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag");
        assertThat(CdpUserTagHistoryDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_user_tag_history");
        assertThat(CdpEventLogDO.class.getAnnotation(TableName.class).value()).isEqualTo("cdp_event_log");
        assertThat(AudienceSnapshotDO.class.getAnnotation(TableName.class).value()).isEqualTo("audience_snapshot");
        assertThat(AudienceDefinitionDO.class.getAnnotation(TableName.class).value()).isEqualTo("audience_definition");
        assertThat(CdpWarehouseIncidentDO.class.getAnnotation(TableName.class).value())
                .isEqualTo("cdp_warehouse_incident");

        assertThat(BaseMapper.class).isAssignableFrom(CdpUserProfileMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(CdpUserIdentityMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(TagDefinitionMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(CdpUserTagMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(CdpUserTagHistoryMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(CdpEventLogMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(AudienceSnapshotMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(AudienceDefinitionMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(CdpWarehouseIncidentMapper.class);
    }

    @Test
    void converterRoundTripsProfileTagEventAndAudienceSnapshotRows() {
        CustomerProfile profile = new CustomerProfile(
                10L,
                7L,
                "u1",
                "Alice",
                "13812345678",
                "alice@example.com",
                "ACTIVE",
                Map.of("tier", "vip"),
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-02T00:00:00"),
                "seed",
                null,
                null);
        CdpUserProfileDO profileRow = converter.toProfileRow(profile);
        assertThat(profileRow.getPropertiesJson()).contains("\"tier\":\"vip\"");
        assertThat(converter.toProfile(profileRow).properties()).containsEntry("tier", "vip");

        TagDefinitionDO definitionRow = new TagDefinitionDO();
        definitionRow.setTagCode("vip");
        definitionRow.setName("VIP");
        definitionRow.setValueType("BOOLEAN");
        definitionRow.setEnabled(1);
        definitionRow.setManualEnabled(1);
        definitionRow.setDefaultTtlDays(3);
        CdpTagDefinition definition = converter.toTagDefinition(definitionRow);
        assertThat(definition.tagCode()).isEqualTo("vip");
        assertThat(definition.manualEnabled()).isTrue();

        CdpUserTagDO tagRow = converter.toUserTagRow(new CdpUserTag(
                11L,
                7L,
                "u1",
                "vip",
                "true",
                "BOOLEAN",
                "MANUAL",
                "req-1",
                "ACTIVE",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                null,
                "admin",
                null,
                null));
        assertThat(tagRow.getTagValue()).isEqualTo("true");
        assertThat(converter.toUserTag(tagRow).tagCode()).isEqualTo("vip");

        CdpEventLogDO eventRow = converter.toEventRow(new CdpEventLog(
                12L,
                7L,
                20L,
                "msg-1",
                "track",
                "OrderComplete",
                "u1",
                "anon-1",
                "sess-1",
                "dev-1",
                "WEB",
                Map.of("library", "sdk"),
                Map.of("amount", 20),
                "idem-1",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                null,
                LocalDateTime.parse("2026-06-01T00:00:01"),
                "ACCEPTED",
                null,
                null));
        assertThat(eventRow.getSdkContext()).contains("\"library\":\"sdk\"");
        assertThat(converter.toEvent(eventRow).properties()).containsEntry("amount", 20);

        AudienceSnapshotDO snapshotRow = converter.toAudienceSnapshotRow(new AudienceSnapshot(
                13L,
                100L,
                200L,
                300L,
                "node-1",
                AudienceSnapshotMode.STATIC_LOCKED,
                List.of("u1", "u2"),
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00")));
        assertThat(snapshotRow.getUserIdsJson()).contains("\"u1\"");
        assertThat(converter.toAudienceSnapshot(snapshotRow).userIds()).containsExactly("u1", "u2");
    }
}
