package com.learningplatform.exam.service;

import com.learningplatform.exam.mapper.ExamAttemptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ExamTimeoutScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExamTimeoutScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final ExamAttemptMapper attemptMapper;
    private final ExamSubmissionService submissionService;

    public ExamTimeoutScheduler(
            ExamAttemptMapper attemptMapper,
            ExamSubmissionService submissionService
    ) {
        this.attemptMapper = attemptMapper;
        this.submissionService = submissionService;
    }

    @Scheduled(
            fixedDelayString = "${app.exam.timeout-scan-ms:5000}",
            initialDelayString = "${app.exam.timeout-scan-initial-delay-ms:5000}"
    )
    public void scan() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        List<Long> attemptIds = attemptMapper.findExpiredIds(now, BATCH_SIZE);
        int submitted = 0;
        for (Long attemptId : attemptIds) {
            try {
                if (submissionService.submitExpired(attemptId)) {
                    submitted++;
                }
            } catch (RuntimeException exception) {
                log.error("Failed to auto-submit expired exam attempt {}", attemptId, exception);
            }
        }
        if (submitted > 0) {
            log.info("Auto-submitted {} expired exam attempt(s)", submitted);
        }
    }
}
