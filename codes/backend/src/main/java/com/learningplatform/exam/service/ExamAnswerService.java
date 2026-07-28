/* 文件职责：实现考试答案业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.domain.ExamAnswer;
import com.learningplatform.exam.domain.ExamAnswerGradingStatus;
import com.learningplatform.exam.domain.ExamAttempt;
import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamPaperQuestion;
import com.learningplatform.exam.dto.ExamAnswerBatchItemRequest;
import com.learningplatform.exam.dto.ExamAnswerResponse;
import com.learningplatform.exam.dto.ExamAnswerWriteRequest;
import com.learningplatform.exam.mapper.ExamAnswerMapper;
import com.learningplatform.exam.mapper.ExamAttemptMapper;
import com.learningplatform.exam.mapper.ExamPaperQuestionMapper;
import com.learningplatform.question.domain.QuestionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
/**
 * 实现考试答案业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamAnswerService {
    /** 访问作答持久化数据。 */
    private final ExamAttemptMapper attemptMapper;
    /** 访问试卷题目持久化数据。 */
    private final ExamPaperQuestionMapper paperQuestionMapper;
    /** 访问答案持久化数据。 */
    private final ExamAnswerMapper answerMapper;
    /** 委托运行态State执行对应领域规则。 */
    private final ExamRuntimeStateService runtimeStateService;
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamAnswerService(
            ExamAttemptMapper attemptMapper,
            ExamPaperQuestionMapper paperQuestionMapper,
            ExamAnswerMapper answerMapper,
            ExamRuntimeStateService runtimeStateService,
            ObjectMapper objectMapper
    ) {
        this.attemptMapper = attemptMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.answerMapper = answerMapper;
        this.runtimeStateService = runtimeStateService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    /** 创建或初始化And列表，并维护唯一性、初始状态和必要关联。 */
    public List<ExamAnswerResponse> initializeAndList(
            ExamAttempt attempt,
            Long paperId,
            LocalDateTime now
    ) {
        for (ExamPaperQuestion question : paperQuestionMapper.findByPaperId(paperId)) {
            ExamAnswer answer = new ExamAnswer();
            answer.setAttemptId(attempt.getId());
            answer.setPaperQuestionId(question.getId());
            answer.setQuestionId(question.getQuestionId());
            answer.setMaxScore(question.getScore());
            answer.setSavedAt(now);
            answerMapper.insertIfAbsent(answer);
        }
        return listResponses(attempt.getId());
    }

    @Transactional
    /** 更新One，通过返回值或版本条件识别并发状态变化。 */
    public ExamAnswerResponse saveOne(
            Long examId,
            Long userId,
            Long questionId,
            ExamAnswerWriteRequest request
    ) {
        ExamAttempt attempt = requireWritableAttempt(examId, userId);
        LocalDateTime now = now();
        ExamAnswer answer = answerMapper.findOne(attempt.getId(), questionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "该题目不属于当前考试"
                ));
        applyAnswer(answer, request, now);
        persist(answer);
        attemptMapper.touchSavedAt(attempt.getId(), now);
        runtimeStateService.rememberSaved(attempt.getId(), now);
        return response(answer);
    }

    @Transactional
    /** 更新Batch，通过返回值或版本条件识别并发状态变化。 */
    public List<ExamAnswerResponse> saveBatch(
            Long examId,
            Long userId,
            List<ExamAnswerBatchItemRequest> requests
    ) {
        ExamAttempt attempt = requireWritableAttempt(examId, userId);
        Set<Long> questionIds = new HashSet<>();
        for (ExamAnswerBatchItemRequest request : requests) {
            if (!questionIds.add(request.questionId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "批量保存中题目不能重复");
            }
        }
        LocalDateTime now = now();
        List<ExamAnswerResponse> responses = new ArrayList<>(requests.size());
        for (ExamAnswerBatchItemRequest request : requests) {
            ExamAnswer answer = answerMapper.findOne(attempt.getId(), request.questionId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "题目 " + request.questionId() + " 不属于当前考试"
                    ));
            applyAnswer(answer, request.answer(), now);
            persist(answer);
            responses.add(response(answer));
        }
        attemptMapper.touchSavedAt(attempt.getId(), now);
        runtimeStateService.rememberSaved(attempt.getId(), now);
        return List.copyOf(responses);
    }

    /** 查询Responses相关数据；只返回当前调用方有权查看的结果。 */
    public List<ExamAnswerResponse> listResponses(Long attemptId) {
        return answerMapper.findByAttemptId(attemptId).stream()
                .map(this::response)
                .toList();
    }

    /** 校验Writable作答及相关业务前置条件，不满足时抛出明确业务异常。 */
    private ExamAttempt requireWritableAttempt(Long examId, Long userId) {
        ExamAttempt attempt = attemptMapper.findFirstForUpdate(examId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "请先开始考试"));
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.CONFLICT, "试卷已提交，不能继续修改答案");
        }
        if (!now().isBefore(attempt.getDeadlineAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "个人作答时间已结束");
        }
        return attempt;
    }

    /** 执行 applyAnswer 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void applyAnswer(
            ExamAnswer answer,
            ExamAnswerWriteRequest request,
            LocalDateTime savedAt
    ) {
        List<String> values = normalizeValues(request.values());
        String text = normalizeText(request.text());
        validate(answer, values, text);
        boolean unanswered = values.stream().allMatch(String::isBlank) && text == null;
        answer.setAnswerJson(values.isEmpty() ? null : encode(values));
        answer.setAnswerText(text);
        answer.setGradingStatus(
                unanswered ? ExamAnswerGradingStatus.UNANSWERED : ExamAnswerGradingStatus.SAVED
        );
        answer.setSavedAt(savedAt);
    }

    /** 校验及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validate(ExamAnswer answer, List<String> values, String text) {
        QuestionType type = answer.getQuestionType();
        if (type == QuestionType.SHORT_ANSWER) {
            if (!values.isEmpty()) {
                throw invalid("简答题只能提交文本答案");
            }
            return;
        }
        if (text != null) {
            throw invalid("当前题型不能提交文本答案");
        }
        if (values.isEmpty()) {
            return;
        }
        if (type == QuestionType.SINGLE_CHOICE && values.size() != 1) {
            throw invalid("单选题只能选择一个答案");
        }
        if (type == QuestionType.MULTIPLE_CHOICE
                && new HashSet<>(values).size() != values.size()) {
            throw invalid("多选题答案不能重复");
        }
        if (type == QuestionType.TRUE_FALSE
                && (values.size() != 1
                || (!"TRUE".equals(values.get(0)) && !"FALSE".equals(values.get(0))))) {
            throw invalid("判断题答案必须是 TRUE 或 FALSE");
        }
        if (type == QuestionType.FILL_BLANK && !values.isEmpty()
                && values.size() != blankCount(answer.getAnswerSnapshot())) {
            throw invalid("填空题答案数量与题目空位数不一致");
        }
        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {
            Set<String> optionKeys = optionKeys(answer.getOptionsSnapshot());
            if (!optionKeys.containsAll(values)) {
                throw invalid("选择题答案包含无效选项");
            }
        }
    }

    /** 执行 optionKeys 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private Set<String> optionKeys(String optionsSnapshot) {
        if (optionsSnapshot == null || optionsSnapshot.isBlank()) {
            return Set.of();
        }
        try {
            JsonNode root = objectMapper.readTree(optionsSnapshot);
            Set<String> keys = new HashSet<>();
            for (JsonNode option : root) {
                keys.add(option.path("key").asText());
            }
            return keys;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试题选项快照损坏");
        }
    }

    /** 执行 blankCount 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private int blankCount(String answerSnapshot) {
        try {
            return objectMapper.readTree(answerSnapshot)
                    .path("acceptedAnswers")
                    .size();
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试题答案快照损坏");
        }
    }

    /** 转换或规范化Values数据，不引入额外持久化副作用。 */
    private List<String> normalizeValues(List<String> source) {
        List<String> values = new ArrayList<>();
        for (String value : source) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.length() > 2000) {
                throw invalid("单项答案不能超过2000个字符");
            }
            values.add(normalized);
        }
        return List.copyOf(values);
    }

    /** 转换或规范化Text数据，不引入额外持久化副作用。 */
    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 20000) {
            throw invalid("文本答案不能超过20000个字符");
        }
        return normalized;
    }

    /** 执行 encode 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String encode(List<String> values) {
        try {
            return objectMapper.writeValueAsString(Map.of("values", values));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答案序列化失败");
        }
    }

    /** 执行 decode 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private List<String> decode(String answerJson) {
        if (answerJson == null || answerJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode values = objectMapper.readTree(answerJson).path("values");
            List<String> result = new ArrayList<>();
            values.forEach(value -> result.add(value.asText()));
            return List.copyOf(result);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答案数据损坏");
        }
    }

    /** 执行 persist 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void persist(ExamAnswer answer) {
        if (answerMapper.updateAnswer(answer) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存答案失败");
        }
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private ExamAnswerResponse response(ExamAnswer answer) {
        return new ExamAnswerResponse(
                answer.getId(),
                answer.getQuestionId(),
                answer.getPaperQuestionId(),
                decode(answer.getAnswerJson()),
                answer.getAnswerText(),
                answer.getGradingStatus(),
                answer.getSavedAt()
        );
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** 执行 invalid 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
