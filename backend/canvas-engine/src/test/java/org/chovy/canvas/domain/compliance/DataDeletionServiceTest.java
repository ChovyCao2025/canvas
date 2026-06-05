package org.chovy.canvas.domain.compliance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataDeletionServiceTest {

    @Test
    void dryRunCountsRowsWithoutDeleting() {
        Fixtures fixtures = fixtures();
        when(fixtures.profileMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.identityMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(fixtures.tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(fixtures.consentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
        when(fixtures.suppressionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(fixtures.messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(6L);
        when(fixtures.traceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);

        DataDeletionService.DeletionResult result =
                fixtures.service.deleteUserData(new DataDeletionService.DeleteUserDataCommand(7L, "user-1", true, "alice"));

        assertThat(result.dryRun()).isTrue();
        assertThat(result.totalMatched()).isEqualTo(28L);
        assertThat(result.tableResults()).extracting(DataDeletionService.TableDeletionResult::tableName)
                .contains("cdp_user_profile", "cdp_user_identity", "cdp_user_tag",
                        "marketing_consent", "marketing_suppression", "message_send_record",
                        "canvas_execution_trace");
        verify(fixtures.profileMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.messageMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.traceMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void executedDeletionDeletesGovernedTables() {
        Fixtures fixtures = fixtures();
        when(fixtures.profileMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.identityMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.consentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.suppressionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.traceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(fixtures.profileMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(fixtures.identityMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(fixtures.tagMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(fixtures.consentMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(fixtures.suppressionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(fixtures.messageMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(fixtures.traceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        DataDeletionService.DeletionResult result =
                fixtures.service.deleteUserData(new DataDeletionService.DeleteUserDataCommand(7L, "user-1", false, "alice"));

        assertThat(result.dryRun()).isFalse();
        assertThat(result.totalDeleted()).isEqualTo(7L);
        verify(fixtures.profileMapper).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.identityMapper).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.tagMapper).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.consentMapper).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.suppressionMapper).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.messageMapper).delete(any(LambdaQueryWrapper.class));
        verify(fixtures.traceMapper).delete(any(LambdaQueryWrapper.class));
    }

    private Fixtures fixtures() {
        CdpUserProfileMapper profileMapper = mock(CdpUserProfileMapper.class);
        CdpUserIdentityMapper identityMapper = mock(CdpUserIdentityMapper.class);
        CdpUserTagMapper tagMapper = mock(CdpUserTagMapper.class);
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        MarketingSuppressionMapper suppressionMapper = mock(MarketingSuppressionMapper.class);
        MessageSendRecordMapper messageMapper = mock(MessageSendRecordMapper.class);
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        return new Fixtures(
                profileMapper,
                identityMapper,
                tagMapper,
                consentMapper,
                suppressionMapper,
                messageMapper,
                traceMapper,
                new DataDeletionService(profileMapper, identityMapper, tagMapper, consentMapper, suppressionMapper, messageMapper, traceMapper));
    }

    private record Fixtures(
            CdpUserProfileMapper profileMapper,
            CdpUserIdentityMapper identityMapper,
            CdpUserTagMapper tagMapper,
            MarketingConsentMapper consentMapper,
            MarketingSuppressionMapper suppressionMapper,
            MessageSendRecordMapper messageMapper,
            CanvasExecutionTraceMapper traceMapper,
            DataDeletionService service) {
    }
}
