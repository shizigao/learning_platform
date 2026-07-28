/* 文件职责：实现考试成绩业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamResult;
import com.learningplatform.exam.dto.ExamResultDetailResponse;
import com.learningplatform.exam.dto.ExamResultQuestionResponse;
import com.learningplatform.exam.mapper.ExamAnswerMapper;
import com.learningplatform.exam.mapper.ExamCandidateMapper;
import com.learningplatform.exam.mapper.ExamResultMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * 实现考试成绩业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamResultService {
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;
    /** 访问考生持久化数据。 */
    private final ExamCandidateMapper candidateMapper;
    /** 访问成绩持久化数据。 */
    private final ExamResultMapper resultMapper;
    /** 访问答案持久化数据。 */
    private final ExamAnswerMapper answerMapper;
    /** 委托presentation执行对应领域规则。 */
    private final ExamAnswerPresentationService presentationService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamResultService(
            ExamService examService,
            ExamCandidateMapper candidateMapper,
            ExamResultMapper resultMapper,
            ExamAnswerMapper answerMapper,
            ExamAnswerPresentationService presentationService
    ) {
        this.examService = examService;
        this.candidateMapper = candidateMapper;
        this.resultMapper = resultMapper;
        this.answerMapper = answerMapper;
        this.presentationService = presentationService;
    }

    /** 判断是否满足didate成绩条件，不修改持久化状态。 */
    public ExamResultDetailResponse candidateResult(Long examId, Long userId) {
        Exam exam = examService.getRequired(examId);
        if (!candidateMapper.exists(examId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你不是本场考试的指定考生");
        }
        ExamResult result = resultMapper.findCandidateResult(examId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "尚未生成考试成绩"));
        if (!Boolean.TRUE.equals(result.getVisibleToCandidate())) {
            throw new BusinessException(ErrorCode.CONFLICT, "主观题批改完成后方可查看成绩");
        }
        boolean answersVisible = Boolean.TRUE.equals(result.getGradingCompleted())
                && Boolean.TRUE.equals(exam.getShowAnswerAfterFinish())
                && !LocalDateTime.now().isBefore(exam.getEndAt());
        List<ExamResultQuestionResponse> questions = answerMapper.findByAttemptId(
                        result.getAttemptId()
                ).stream()
                .map(answer -> presentationService.response(answer, answersVisible))
                .toList();
        return new ExamResultDetailResponse(
                com.learningplatform.exam.dto.ExamResultSummaryResponse.from(result, questions),
                answersVisible,
                questions
        );
    }
}
