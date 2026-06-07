package org.chovy.canvas.domain.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryDO;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryVersionDO;
import org.chovy.canvas.dal.mapper.MarketingContentEntryMapper;
import org.chovy.canvas.dal.mapper.MarketingContentEntryVersionMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentEntryServiceTest {

    @Test
    void saveDraftNormalizesSlugAndWritesVersion() {
        MarketingContentEntryMapper entryMapper = mock(MarketingContentEntryMapper.class);
        MarketingContentEntryVersionMapper versionMapper = mock(MarketingContentEntryVersionMapper.class);
        ContentEntryService service = new ContentEntryService(entryMapper, versionMapper, new ObjectMapper());

        ContentEntryService.EntryView view = service.saveDraft(operator(), new ContentEntryService.EntryCommand(
                " Summer Landing ",
                "LANDING_PAGE",
                "Summer Landing",
                " /Summer Sale ",
                "en-US",
                "Season offer",
                "{\"blocks\":[{\"type\":\"hero\"}]}",
                "{\"title\":\"Summer\"}",
                "[\"hero-video\"]",
                "operator-1"));

        assertThat(view.entryKey()).isEqualTo("summer-landing");
        assertThat(view.slug()).isEqualTo("summer-sale");
        assertThat(view.status()).isEqualTo("DRAFT");
        verify(entryMapper).insert(argThat((MarketingContentEntryDO row) ->
                row.getTenantId().equals(8L)
                        && row.getEntryKey().equals("summer-landing")
                        && row.getSlug().equals("summer-sale")
                        && row.getStatus().equals("DRAFT")));
        verify(versionMapper).insert(argThat((MarketingContentEntryVersionDO row) ->
                row.getTenantId().equals(8L)
                        && row.getEntryKey().equals("summer-landing")
                        && row.getVersionNo().equals(1)
                        && row.getStatus().equals("DRAFT")));
    }

    @Test
    void publishAndArchiveWriteVersionRows() {
        MarketingContentEntryMapper entryMapper = mock(MarketingContentEntryMapper.class);
        MarketingContentEntryVersionMapper versionMapper = mock(MarketingContentEntryVersionMapper.class);
        when(entryMapper.selectOne(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ContentEntryServiceTestRows.entry("summer-landing", "DRAFT"))
                .thenReturn(ContentEntryServiceTestRows.entry("summer-landing", "PUBLISHED"));
        ContentEntryService service = new ContentEntryService(entryMapper, versionMapper, new ObjectMapper());

        assertThat(service.publish(operator(), "summer-landing", new ContentEntryService.EntryStatusCommand("operator-2")).status())
                .isEqualTo("PUBLISHED");
        assertThat(service.archive(operator(), "summer-landing", new ContentEntryService.EntryStatusCommand("operator-2")).status())
                .isEqualTo("ARCHIVED");

        verify(versionMapper).insert(argThat((MarketingContentEntryVersionDO row) -> row.getStatus().equals("PUBLISHED")));
        verify(versionMapper).insert(argThat((MarketingContentEntryVersionDO row) -> row.getStatus().equals("ARCHIVED")));
    }

    @Test
    void saveDraftRejectsInvalidBodyJson() {
        ContentEntryService service = new ContentEntryService(
                mock(MarketingContentEntryMapper.class),
                mock(MarketingContentEntryVersionMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.saveDraft(operator(), new ContentEntryService.EntryCommand(
                "bad", "ARTICLE", "Bad", "bad", "zh-CN", null, "{bad", "{}", "[]", "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bodyJson must be valid JSON");

        assertThatThrownBy(() -> service.saveDraft(operator(), new ContentEntryService.EntryCommand(
                "bad", "ARTICLE", "Bad", "bad", "zh-CN", null, "[]", "{}", "[]", "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bodyJson must be a JSON object");

        assertThatThrownBy(() -> service.saveDraft(operator(), new ContentEntryService.EntryCommand(
                "bad", "ARTICLE", "Bad", "bad", "zh-CN", null, "{}", "[]", "[]", "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seoJson must be a JSON object");

        assertThatThrownBy(() -> service.saveDraft(operator(), new ContentEntryService.EntryCommand(
                "bad", "ARTICLE", "Bad", "bad", "zh-CN", null, "{}", "{}", "{}", "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assetRefsJson must be a JSON array");
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
