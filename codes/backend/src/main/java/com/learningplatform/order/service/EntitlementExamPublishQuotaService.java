package com.learningplatform.order.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.service.ExamPublishQuotaService;
import com.learningplatform.order.domain.EntitlementType;
import org.springframework.stereotype.Service;

@Service
public class EntitlementExamPublishQuotaService implements ExamPublishQuotaService {
    private final EntitlementService entitlementService;

    public EntitlementExamPublishQuotaService(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @Override
    public void consume(Long publisherId, Long examId) {
        try {
            entitlementService.consumeQuota(
                    publisherId,
                    EntitlementType.EXAM_QUOTA,
                    1
            );
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.FORBIDDEN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "考试发布额度不足");
            }
            throw exception;
        }
    }

    @Override
    public int availableQuota(Long publisherId) {
        return entitlementService.availableQuota(
                publisherId,
                EntitlementType.EXAM_QUOTA
        );
    }
}
