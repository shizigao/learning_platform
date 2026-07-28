/* 文件职责：实现题目题库业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：题库、题目、选项与标准答案；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现题目题库业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class QuestionBankService {
    /** 访问题库持久化数据。 */
    private final QuestionBankMapper bankMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public QuestionBankService(QuestionBankMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public List<QuestionBankResponse> list(Long ownerId) {
        return bankMapper.findByOwnerId(ownerId).stream()
                .map(QuestionBankResponse::from)
                .toList();
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
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
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
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
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
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

    /** 返回Required。 */
    public QuestionBank getRequired(Long bankId) {
        return bankMapper.findById(bankId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "题库不存在"));
    }

    /** 校验OwnerOr管理及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void assertOwnerOrAdmin(QuestionBank bank, Long requesterId, boolean requesterAdmin) {
        if (!requesterAdmin && !bank.getOwnerId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的题库");
        }
    }

    /** 执行 apply 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void apply(QuestionBank bank, QuestionBankWriteRequest request) {
        bank.setName(request.name().trim());
        bank.setDescription(normalize(request.description()));
        bank.setStatus(request.status() == null ? QuestionStatus.ACTIVE : request.status());
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
