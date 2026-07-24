package com.learningplatform.question.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.question.domain.QuestionBank;
import com.learningplatform.question.domain.QuestionStatus;
import com.learningplatform.question.dto.QuestionBankResponse;
import com.learningplatform.question.dto.QuestionBankWriteRequest;
import com.learningplatform.question.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionBankService {
    private final QuestionBankMapper bankMapper;

    public QuestionBankService(QuestionBankMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    public List<QuestionBankResponse> list(Long ownerId) {
        return bankMapper.findByOwnerId(ownerId).stream()
                .map(QuestionBankResponse::from)
                .toList();
    }

    @Transactional
    public QuestionBankResponse create(Long ownerId, QuestionBankWriteRequest request) {
        QuestionBank bank = new QuestionBank();
        bank.setOwnerId(ownerId);
        apply(bank, request);
        if (bankMapper.insert(bank) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建题库失败");
        }
        return QuestionBankResponse.from(getRequired(bank.getId()));
    }

    @Transactional
    public QuestionBankResponse update(
            Long bankId,
            Long requesterId,
            boolean requesterAdmin,
            QuestionBankWriteRequest request
    ) {
        QuestionBank bank = getRequired(bankId);
        assertOwnerOrAdmin(bank, requesterId, requesterAdmin);
        apply(bank, request);
        if (bankMapper.update(bank) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题库不存在");
        }
        return QuestionBankResponse.from(getRequired(bankId));
    }

    @Transactional
    public void delete(Long bankId, Long requesterId, boolean requesterAdmin) {
        QuestionBank bank = getRequired(bankId);
        assertOwnerOrAdmin(bank, requesterId, requesterAdmin);
        if (bankMapper.countQuestions(bankId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "题库中仍有题目，不能删除");
        }
        if (bankMapper.softDelete(bankId) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题库不存在");
        }
    }

    public QuestionBank getRequired(Long bankId) {
        return bankMapper.findById(bankId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "题库不存在"));
    }

    public void assertOwnerOrAdmin(QuestionBank bank, Long requesterId, boolean requesterAdmin) {
        if (!requesterAdmin && !bank.getOwnerId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的题库");
        }
    }

    private void apply(QuestionBank bank, QuestionBankWriteRequest request) {
        bank.setName(request.name().trim());
        bank.setDescription(normalize(request.description()));
        bank.setStatus(request.status() == null ? QuestionStatus.ACTIVE : request.status());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
