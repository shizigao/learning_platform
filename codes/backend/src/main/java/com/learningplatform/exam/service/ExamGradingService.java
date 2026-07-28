/* 文件职责：实现考试阅卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamAnswer;
import com.learningplatform.exam.domain.ExamAnswerGradingStatus;
import com.learningplatform.exam.domain.ExamAttempt;
import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamResult;
import com.learningplatform.exam.dto.ExamGradingAttemptResponse;
import com.learningplatform.exam.dto.ExamGradingDetailResponse;
import com.learningplatform.exam.dto.ExamResultQuestionResponse;
import com.learningplatform.exam.dto.ExamResultSummaryResponse;
import com.learningplatform.exam.dto.ManualGradeRequest;
import com.learningplatform.exam.mapper.ExamAnswerMapper;
import com.learningplatform.exam.mapper.ExamAttemptMapper;
import com.learningplatform.exam.mapper.ExamResultMapper;
import com.learningplatform.question.domain.QuestionType;
import com.learningplatform.question.dto.QuestionAnswer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
/**
 * 实现考试阅卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamGradingService {
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;
    /** 访问答案持久化数据。 */
    private final ExamAnswerMapper answerMapper;
    /** 访问作答持久化数据。 */
    private final ExamAttemptMapper attemptMapper;
    /** 访问成绩持久化数据。 */
    private final ExamResultMapper resultMapper;
    /** 委托presentation执行对应领域规则。 */
    private final ExamAnswerPresentationService presentationService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamGradingService(
            ExamService examService,
            ExamAnswerMapper answerMapper,
            ExamAttemptMapper attemptMapper,
            ExamResultMapper resultMapper,
            ExamAnswerPresentationService presentationService
    ) {
        this.examService = examService;
        this.answerMapper = answerMapper;
        this.attemptMapper = attemptMapper;
        this.resultMapper = resultMapper;
        this.presentationService = presentationService;
    }

    @Transactional
    /** 执行评分提交作答核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    public ExamAttempt gradeSubmittedAttempt(ExamAttempt attempt, Exam exam) {
        LocalDateTime gradedAt = now();
        for (ExamAnswer answer : answerMapper.findByAttemptId(attempt.getId())) {
            gradeAutomatically(answer, gradedAt);
            if (answerMapper.updateAutomaticGrade(answer) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "自动评分保存失败");
            }
        }
        refreshResult(attempt, exam, true);
        return attemptMapper.findById(attempt.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "作答记录不存在"));
    }

    /** 查询Attempts相关数据；只返回当前调用方有权查看的结果。 */
    public List<ExamGradingAttemptResponse> listAttempts(
            Long examId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        authorize(examId, requesterId, requesterAdmin);
        return attemptMapper.findSubmittedByExam(examId).stream()
                .map(ExamGradingAttemptResponse::from)
                .toList();
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public ExamGradingDetailResponse detail(
            Long examId,
            Long attemptId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        authorize(examId, requesterId, requesterAdmin);
        ExamAttempt meta = requireAttemptMeta(examId, attemptId);
        List<ExamResultQuestionResponse> questions = answerMapper.findByAttemptId(attemptId)
                .stream()
                .map(answer -> presentationService.response(answer, true))
                .toList();
        return new ExamGradingDetailResponse(ExamGradingAttemptResponse.from(meta), questions);
    }

    @Transactional
    /** 执行评分答案核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    public ExamResultQuestionResponse gradeAnswer(
            Long examId,
            Long attemptId,
            Long answerId,
            Long graderId,
            boolean graderAdmin,
            ManualGradeRequest request
    ) {
        Exam exam = authorize(examId, graderId, graderAdmin);
        ExamAttempt attempt = requireReviewableAttempt(examId, attemptId);
        ExamAnswer answer = answerMapper.findByIdAndAttempt(answerId, attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "答题记录不存在"));
        if (answer.getQuestionType() != QuestionType.SHORT_ANSWER
                && !(answer.getQuestionType() == QuestionType.FILL_BLANK
                && !presentationService.fillBlankAutoGradable(answer))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前题目不需要人工阅卷");
        }
        if (answer.getGradingStatus() != ExamAnswerGradingStatus.PENDING_REVIEW
                && answer.getGradingStatus() != ExamAnswerGradingStatus.GRADED) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前答案不处于待阅卷状态");
        }
        if (request.score().compareTo(answer.getMaxScore()) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "得分不能超过本题满分");
        }

        answer.setScore(request.score().setScale(2, RoundingMode.HALF_UP));
        answer.setCorrect(answer.getScore().compareTo(answer.getMaxScore()) == 0);
        answer.setGraderId(graderId);
        answer.setGraderComment(normalize(request.comment()));
        answer.setGradedAt(now());
        answer.setGradingStatus(ExamAnswerGradingStatus.GRADED);
        if (answerMapper.updateManualGrade(answer) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "答案状态已发生变化，请刷新后重试");
        }
        refreshResult(attempt, exam, false);
        return presentationService.response(answer, true);
    }

    @Transactional
    /** 执行完成状态流转，仅允许从合法前置状态进入目标状态。 */
    public ExamResultSummaryResponse completeReview(
            Long examId,
            Long attemptId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        Exam exam = authorize(examId, requesterId, requesterAdmin);
        ExamAttempt attempt = requireReviewableAttempt(examId, attemptId);
        if (answerMapper.countPendingReview(attemptId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "仍有主观题尚未批改");
        }
        ExamResult result = refreshResult(attempt, exam, true);
        List<ExamResultQuestionResponse> questions = answerMapper.findByAttemptId(attemptId)
                .stream()
                .map(answer -> presentationService.response(answer, true))
                .toList();
        return ExamResultSummaryResponse.from(result, questions);
    }

    /** 执行评分Automatically核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    private void gradeAutomatically(ExamAnswer answer, LocalDateTime gradedAt) {
        List<String> values = presentationService.readValues(answer.getAnswerJson());
        boolean unanswered = answer.getQuestionType() == QuestionType.SHORT_ANSWER
                ? answer.getAnswerText() == null || answer.getAnswerText().isBlank()
                : values.stream().allMatch(String::isBlank);
        if (values.isEmpty() && answer.getQuestionType() != QuestionType.SHORT_ANSWER) {
            unanswered = true;
        }
        if (unanswered) {
            applyGrade(answer, BigDecimal.ZERO, null, ExamAnswerGradingStatus.UNANSWERED, gradedAt);
            return;
        }

        switch (answer.getQuestionType()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE -> gradeChoice(answer, values, gradedAt);
            case FILL_BLANK -> {
                if (presentationService.fillBlankAutoGradable(answer)) {
                    gradeFillBlank(answer, values, gradedAt);
                } else {
                    applyGrade(answer, null, null, ExamAnswerGradingStatus.PENDING_REVIEW, null);
                }
            }
            case SHORT_ANSWER ->
                    applyGrade(answer, null, null, ExamAnswerGradingStatus.PENDING_REVIEW, null);
        }
    }

    /** 执行评分Choice核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    private void gradeChoice(ExamAnswer answer, List<String> values, LocalDateTime gradedAt) {
        QuestionAnswer expected = presentationService.readCorrectAnswer(answer.getAnswerSnapshot());
        Set<String> actualValues = new HashSet<>(values);
        Set<String> expectedValues = expected.acceptedAnswers().isEmpty()
                ? Set.of()
                : new HashSet<>(expected.acceptedAnswers().get(0));
        boolean correct = actualValues.equals(expectedValues);
        applyGrade(
                answer,
                correct ? answer.getMaxScore() : BigDecimal.ZERO,
                correct,
                ExamAnswerGradingStatus.AUTO_GRADED,
                gradedAt
        );
    }

    /** 执行评分FillBlank核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    private void gradeFillBlank(ExamAnswer answer, List<String> values, LocalDateTime gradedAt) {
        QuestionAnswer expected = presentationService.readCorrectAnswer(answer.getAnswerSnapshot());
        int total = expected.acceptedAnswers().size();
        if (total == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "填空题答案快照为空");
        }
        int correctBlanks = 0;
        for (int index = 0; index < total; index++) {
            String actual = index < values.size() ? values.get(index).trim() : "";
            Set<String> accepted = Set.copyOf(expected.acceptedAnswers().get(index));
            boolean matched = accepted.stream().anyMatch(value -> equalsFillAnswer(
                    actual,
                    value.trim(),
                    presentationService.caseSensitive(answer)
            ));
            if (matched) {
                correctBlanks++;
            }
        }
        boolean correct = correctBlanks == total;
        BigDecimal score = answer.getMaxScore()
                .multiply(BigDecimal.valueOf(correctBlanks))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        applyGrade(answer, score, correct, ExamAnswerGradingStatus.AUTO_GRADED, gradedAt);
    }

    /** 执行 equalsFillAnswer 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private boolean equalsFillAnswer(String actual, String expected, boolean caseSensitive) {
        return caseSensitive
                ? actual.equals(expected)
                : actual.toLowerCase(Locale.ROOT).equals(expected.toLowerCase(Locale.ROOT));
    }

    /** 执行 applyGrade 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void applyGrade(
            ExamAnswer answer,
            BigDecimal score,
            Boolean correct,
            ExamAnswerGradingStatus status,
            LocalDateTime gradedAt
    ) {
        answer.setScore(score == null ? null : score.setScale(2, RoundingMode.HALF_UP));
        answer.setCorrect(correct);
        answer.setGradingStatus(status);
        answer.setGradedAt(gradedAt);
    }

    /** 更新成绩，通过返回值或版本条件识别并发状态变化。 */
    private ExamResult refreshResult(ExamAttempt attempt, Exam exam, boolean allowCompletion) {
        List<ExamAnswer> answers = answerMapper.findByAttemptId(attempt.getId());
        BigDecimal objectiveScore = sumByStatus(answers, ExamAnswerGradingStatus.AUTO_GRADED);
        BigDecimal subjectiveScore = sumByStatus(answers, ExamAnswerGradingStatus.GRADED);
        BigDecimal finalScore = answers.stream()
                .map(ExamAnswer::getScore)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int pending = (int) answers.stream()
                .filter(answer -> answer.getGradingStatus() == ExamAnswerGradingStatus.PENDING_REVIEW)
                .count();
        boolean completed = allowCompletion && pending == 0;
        ExamAttemptStatus targetStatus = completed
                ? ExamAttemptStatus.COMPLETED
                : ExamAttemptStatus.GRADING;
        if (attemptMapper.updateGradingState(
                attempt.getId(),
                targetStatus,
                objectiveScore,
                subjectiveScore,
                finalScore
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "评分状态已发生变化，请重试");
        }

        ExamResult result = resultMapper.findByAttemptId(attempt.getId()).orElseGet(ExamResult::new);
        result.setExamId(attempt.getExamId());
        result.setAttemptId(attempt.getId());
        result.setUserId(attempt.getUserId());
        result.setTotalScore(finalScore);
        result.setPassingScore(exam.getPassingScore());
        result.setPassed(finalScore.compareTo(exam.getPassingScore()) >= 0);
        result.setCorrectCount((int) answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                .count());
        result.setIncorrectCount((int) answers.stream()
                .filter(answer -> answer.getGradingStatus() != ExamAnswerGradingStatus.UNANSWERED)
                .filter(answer -> Boolean.FALSE.equals(answer.getCorrect()))
                .filter(answer -> answer.getScore() != null)
                .filter(answer -> answer.getScore().compareTo(BigDecimal.ZERO) == 0)
                .count());
        result.setUnansweredCount((int) answers.stream()
                .filter(answer -> answer.getGradingStatus() == ExamAnswerGradingStatus.UNANSWERED)
                .count());
        result.setGradingCompleted(completed);
        result.setVisibleToCandidate(Boolean.TRUE.equals(exam.getShowResultImmediately()) || completed);
        result.setGeneratedAt(now());
        if (result.getId() == null) {
            if (resultMapper.insert(result) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成考试成绩失败");
            }
        } else if (resultMapper.update(result) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新考试成绩失败");
        }
        return result;
    }

    /** 执行 sumByStatus 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BigDecimal sumByStatus(
            List<ExamAnswer> answers,
            ExamAnswerGradingStatus status
    ) {
        return answers.stream()
                .filter(answer -> answer.getGradingStatus() == status)
                .map(ExamAnswer::getScore)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 校验授权及相关业务前置条件，不满足时抛出明确业务异常。 */
    private Exam requireAuthorization(
            Long examId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        examService.detail(examId, requesterId, requesterAdmin);
        return examService.getRequired(examId);
    }

    /** 执行 authorize 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private Exam authorize(Long examId, Long requesterId, boolean requesterAdmin) {
        return requireAuthorization(examId, requesterId, requesterAdmin);
    }

    /** 校验Reviewable作答及相关业务前置条件，不满足时抛出明确业务异常。 */
    private ExamAttempt requireReviewableAttempt(Long examId, Long attemptId) {
        ExamAttempt attempt = attemptMapper.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "作答记录不存在"));
        if (!attempt.getExamId().equals(examId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作答记录不属于当前考试");
        }
        if (attempt.getStatus() == ExamAttemptStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONFLICT, "本次阅卷已经完成");
        }
        if (attempt.getStatus() != ExamAttemptStatus.GRADING) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前作答记录不能人工阅卷");
        }
        return attempt;
    }

    /** 校验作答Meta及相关业务前置条件，不满足时抛出明确业务异常。 */
    private ExamAttempt requireAttemptMeta(Long examId, Long attemptId) {
        return attemptMapper.findSubmittedByExam(examId).stream()
                .filter(attempt -> attempt.getId().equals(attemptId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "作答记录不存在"));
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
