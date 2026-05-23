package org.chovy.canvas.domain.meta;

import org.chovy.canvas.dto.TagImportResult;
import org.chovy.canvas.dto.TagImportRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagImportServiceTest {

    @Mock
    private IdentityTypeService identityTypeService;

    @Mock
    private TagDefinitionService tagDefinitionService;

    @Mock
    private UserTagCurrentMapper userTagCurrentMapper;

    @Mock
    private TagImportBatchMapper tagImportBatchMapper;

    @Mock
    private TagImportErrorMapper tagImportErrorMapper;

    @Test
    void importRows_validRow_insertsCurrentTag_andReturnsSuccess() {
        TagImportService service = new TagImportService(
                identityTypeService, tagDefinitionService, userTagCurrentMapper, tagImportBatchMapper, tagImportErrorMapper);
        mockBatchId();

        TagImportRow row = new TagImportRow();
        row.setRowNo(1);
        row.setIdType("email");
        row.setIdValue("user@example.com");
        row.setTagCode("tier");
        row.setTagValue("vip");
        row.setTagTime(LocalDateTime.of(2026, 5, 23, 10, 30));

        TagImportResult result = service.importRows("API_PUSH", null, null, List.of(row));

        assertThat(result.getBatchId()).isEqualTo(1001L);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEqualTo(0);

        ArgumentCaptor<UserTagCurrent> currentCaptor = ArgumentCaptor.forClass(UserTagCurrent.class);
        verify(userTagCurrentMapper).insert((UserTagCurrent) currentCaptor.capture());
        UserTagCurrent inserted = currentCaptor.getValue();
        assertThat(inserted.getIdType()).isEqualTo("email");
        assertThat(inserted.getIdValue()).isEqualTo("user@example.com");
        assertThat(inserted.getTagCode()).isEqualTo("tier");
        assertThat(inserted.getTagValue()).isEqualTo("vip");
        assertThat(inserted.getTagTime()).isEqualTo(LocalDateTime.of(2026, 5, 23, 10, 30));
        assertThat(inserted.getSourceType()).isEqualTo("API_PUSH");
        assertThat(inserted.getSourceBatchId()).isEqualTo(1001L);

        verify(tagImportErrorMapper, never()).insert(any(TagImportError.class));
    }

    @Test
    void importRows_duplicateRowInSameBatch_writesError_andReturnsPartialSuccess() {
        TagImportService service = new TagImportService(
                identityTypeService, tagDefinitionService, userTagCurrentMapper, tagImportBatchMapper, tagImportErrorMapper);
        mockBatchId();

        TagImportRow first = new TagImportRow();
        first.setRowNo(1);
        first.setIdType("email");
        first.setIdValue("user@example.com");
        first.setTagCode("tier");
        first.setTagValue("vip");

        TagImportRow duplicate = new TagImportRow();
        duplicate.setRowNo(2);
        duplicate.setIdType("email");
        duplicate.setIdValue("user@example.com");
        duplicate.setTagCode("tier");
        duplicate.setTagValue("normal");

        TagImportResult result = service.importRows("API_PUSH", null, null, List.of(first, duplicate));

        assertThat(result.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getSuccessRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEqualTo(1);

        ArgumentCaptor<TagImportError> errorCaptor = ArgumentCaptor.forClass(TagImportError.class);
        verify(tagImportErrorMapper).insert((TagImportError) errorCaptor.capture());
        TagImportError error = errorCaptor.getValue();
        assertThat(error.getBatchId()).isEqualTo(1001L);
        assertThat(error.getRowNo()).isEqualTo(2);
        assertThat(error.getErrorCode()).isEqualTo("DUPLICATE_ROW");
        assertThat(error.getErrorMsg()).contains("duplicate");
        assertThat(error.getRawPayload()).contains("email");
        assertThat(error.getRawPayload()).contains("tier");
    }

    @Test
    void importRows_existingCurrentTag_updatesInsteadOfInsert() {
        TagImportService service = new TagImportService(
                identityTypeService, tagDefinitionService, userTagCurrentMapper, tagImportBatchMapper, tagImportErrorMapper);
        mockBatchId();

        UserTagCurrent existing = new UserTagCurrent();
        existing.setId(88L);
        existing.setIdType("email");
        existing.setIdValue("user@example.com");
        existing.setTagCode("tier");
        existing.setTagValue("normal");
        when(userTagCurrentMapper.selectOne(any())).thenReturn(existing);

        TagImportRow row = new TagImportRow();
        row.setRowNo(1);
        row.setIdType("email");
        row.setIdValue("user@example.com");
        row.setTagCode("tier");
        row.setTagValue("vip");
        row.setTagTime(LocalDateTime.of(2026, 5, 23, 11, 0));

        TagImportResult result = service.importRows("API_PUSH", null, null, List.of(row));

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(userTagCurrentMapper, never()).insert(any(UserTagCurrent.class));

        ArgumentCaptor<UserTagCurrent> currentCaptor = ArgumentCaptor.forClass(UserTagCurrent.class);
        verify(userTagCurrentMapper).updateById(currentCaptor.capture());
        UserTagCurrent updated = currentCaptor.getValue();
        assertThat(updated.getId()).isEqualTo(88L);
        assertThat(updated.getTagValue()).isEqualTo("vip");
        assertThat(updated.getTagTime()).isEqualTo(LocalDateTime.of(2026, 5, 23, 11, 0));
        assertThat(updated.getSourceType()).isEqualTo("API_PUSH");
        assertThat(updated.getSourceBatchId()).isEqualTo(1001L);
    }

    private void mockBatchId() {
        doAnswer(invocation -> {
            TagImportBatch batch = invocation.getArgument(0);
            batch.setId(1001L);
            return 1;
        }).when(tagImportBatchMapper).insert(any(TagImportBatch.class));
    }
}
