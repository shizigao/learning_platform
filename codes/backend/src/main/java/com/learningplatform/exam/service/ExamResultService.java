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
public class ExamResultService {
    private final ExamService examService;
    private final ExamCandidateMapper candidateMapper;
    private final ExamResultMapper resultMapper;
    private final ExamAnswerMapper answerMapper;
    private final ExamAnswerPresentationService presentationService;

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
