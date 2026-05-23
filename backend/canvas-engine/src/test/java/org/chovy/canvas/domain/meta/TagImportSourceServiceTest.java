package org.chovy.canvas.domain.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dto.TagImportRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TagImportSourceServiceTest {

    @Mock
    private TagImportSourceMapper tagImportSourceMapper;

    @Mock
    private TagImportService tagImportService;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Test
    void mapRows_mapsConfiguredFields_andUsesOneBasedRowNumbers() {
        TagImportSourceService service = new TagImportSourceService(
                tagImportSourceMapper, tagImportService, new ObjectMapper(), webClientBuilder);

        TagImportSource source = new TagImportSource();
        source.setFieldMapping("""
                {"idType":"identity_type","idValue":"identity_value","tagCode":"tag_code","tagValue":"tag_value","tagTime":"tag_time"}
                """);

        List<Map<String, Object>> records = List.of(
                Map.of(
                        "identity_type", "email",
                        "identity_value", "first@example.com",
                        "tag_code", "tier",
                        "tag_value", "vip",
                        "tag_time", "2026-05-23 10:30:00"
                ),
                Map.of(
                        "identity_type", "phone",
                        "identity_value", "13800138000",
                        "tag_code", "member_level",
                        "tag_value", "gold",
                        "tag_time", "2026-05-23 11:45:00"
                )
        );

        List<TagImportRow> rows = service.mapRows(source, records);

        assertThat(rows).hasSize(2);

        TagImportRow first = rows.get(0);
        assertThat(first.getRowNo()).isEqualTo(1);
        assertThat(first.getIdType()).isEqualTo("email");
        assertThat(first.getIdValue()).isEqualTo("first@example.com");
        assertThat(first.getTagCode()).isEqualTo("tier");
        assertThat(first.getTagValue()).isEqualTo("vip");
        assertThat(first.getTagTime()).isEqualTo(LocalDateTime.of(2026, 5, 23, 10, 30));

        TagImportRow second = rows.get(1);
        assertThat(second.getRowNo()).isEqualTo(2);
        assertThat(second.getIdType()).isEqualTo("phone");
        assertThat(second.getIdValue()).isEqualTo("13800138000");
        assertThat(second.getTagCode()).isEqualTo("member_level");
        assertThat(second.getTagValue()).isEqualTo("gold");
        assertThat(second.getTagTime()).isEqualTo(LocalDateTime.of(2026, 5, 23, 11, 45));
    }
}
