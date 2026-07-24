package com.learningplatform.exam.service;

public interface ExamPublishQuotaService {
    void consume(Long publisherId, Long examId);

    int availableQuota(Long publisherId);
}
