package com.learningplatform.exam.service;

import com.learningplatform.exam.mapper.ExamAttemptMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamTimeoutSchedulerTests {

    @Mock
    private ExamAttemptMapper attemptMapper;

    @Mock
    private ExamSubmissionService submissionService;

    @Mock
    private ExamRuntimeStateService runtimeStateService;

    @Test
    void submitsAttemptClaimedFromRedisWithoutQueryingMysql() {
        when(runtimeStateService.claimExpired(any(LocalDateTime.class), eq(100)))
                .thenReturn(new ExamRuntimeStateService.ExpiredAttemptClaim(List.of(42L), true));
        when(submissionService.submitExpired(42L)).thenReturn(true);

        scheduler().scan();

        verify(submissionService).submitExpired(42L);
        verify(attemptMapper, never()).findExpiredIds(any(LocalDateTime.class), anyInt());
    }

    @Test
    void requeuesClaimWhenSubmissionFails() {
        when(runtimeStateService.claimExpired(any(LocalDateTime.class), eq(100)))
                .thenReturn(new ExamRuntimeStateService.ExpiredAttemptClaim(List.of(42L), true));
        when(submissionService.submitExpired(42L))
                .thenThrow(new IllegalStateException("temporary database failure"));

        scheduler().scan();

        verify(runtimeStateService).requeueExpired(eq(42L), any(LocalDateTime.class));
    }

    @Test
    void immediatelyFallsBackToMysqlWhenRedisIsUnavailable() {
        when(runtimeStateService.claimExpired(any(LocalDateTime.class), eq(100)))
                .thenReturn(new ExamRuntimeStateService.ExpiredAttemptClaim(List.of(), false));
        when(attemptMapper.findExpiredIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(51L));
        when(submissionService.submitExpired(51L)).thenReturn(true);

        scheduler().scan();

        verify(submissionService).submitExpired(51L);
    }

    @Test
    void periodicallyScansMysqlToRecoverLostRedisEntries() {
        when(runtimeStateService.claimExpired(any(LocalDateTime.class), eq(100)))
                .thenReturn(new ExamRuntimeStateService.ExpiredAttemptClaim(List.of(), true));
        when(attemptMapper.findExpiredIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of());
        ExamTimeoutScheduler scheduler = scheduler();

        for (int scan = 0; scan < 12; scan++) {
            scheduler.scan();
        }

        verify(attemptMapper, times(1)).findExpiredIds(any(LocalDateTime.class), eq(100));
    }

    private ExamTimeoutScheduler scheduler() {
        return new ExamTimeoutScheduler(attemptMapper, submissionService, runtimeStateService);
    }
}
