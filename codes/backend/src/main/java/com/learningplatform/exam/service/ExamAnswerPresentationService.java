/* 文件职责：实现考试答案Presentation业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.domain.ExamAnswer;
import com.learningplatform.exam.dto.ExamResultQuestionResponse;
import com.learningplatform.question.dto.QuestionAnswer;
import com.learningplatform.question.dto.QuestionOptionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
/**
 * 实现考试答案Presentation业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamAnswerPresentationService {
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamAnswerPresentationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ExamResultQuestionResponse response(ExamAnswer answer, boolean revealAnswer) {
        return new ExamResultQuestionResponse(
                answer.getId(),
                answer.getQuestionId(),
                answer.getSortOrder(),
                answer.getQuestionType(),
                answer.getStemSnapshot(),
                readOptions(answer.getOptionsSnapshot()),
                answer.getMaxScore(),
                readValues(answer.getAnswerJson()),
                answer.getAnswerText(),
                answer.getScore(),
                answer.getCorrect(),
                answer.getGradingStatus(),
                revealAnswer ? readCorrectAnswer(answer.getAnswerSnapshot()) : null,
                revealAnswer ? answer.getAnalysisSnapshot() : null,
                answer.getGraderComment()
        );
    }

    /** 查询Values相关数据；只返回当前调用方有权查看的结果。 */
    public List<String> readValues(String answerJson) {
        if (answerJson == null || answerJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode values = objectMapper.readTree(answerJson).path("values");
            List<String> result = new ArrayList<>();
            values.forEach(value -> result.add(value.asText()));
            return List.copyOf(result);
        } catch (JsonProcessingException exception) {
            throw damaged("考生答案数据损坏");
        }
    }

    /** 查询Correct答案相关数据；只返回当前调用方有权查看的结果。 */
    public QuestionAnswer readCorrectAnswer(String snapshot) {
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            return new QuestionAnswer(objectMapper.convertValue(
                    root.path("acceptedAnswers"),
                    new TypeReference<List<List<String>>>() {
                    }
            ));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw damaged("试题正确答案快照损坏");
        }
    }

    /** 执行 fillBlankAutoGradable 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public boolean fillBlankAutoGradable(ExamAnswer answer) {
        return snapshotBoolean(
                answer.getAnswerSnapshot(),
                "fillBlankAutoGradable",
                Boolean.TRUE.equals(answer.getFillBlankAutoGradable())
        );
    }

    /** 执行 caseSensitive 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public boolean caseSensitive(ExamAnswer answer) {
        return snapshotBoolean(
                answer.getAnswerSnapshot(),
                "caseSensitive",
                Boolean.TRUE.equals(answer.getCaseSensitive())
        );
    }

    /** 查询Options相关数据；只返回当前调用方有权查看的结果。 */
    private List<QuestionOptionResponse> readOptions(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    snapshot,
                    new TypeReference<List<QuestionOptionResponse>>() {
                    }
            );
        } catch (JsonProcessingException exception) {
            throw damaged("试题选项快照损坏");
        }
    }

    /** 执行 snapshotBoolean 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private boolean snapshotBoolean(String snapshot, String field, boolean fallback) {
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            return root.has(field) ? root.path(field).asBoolean() : fallback;
        } catch (JsonProcessingException exception) {
            throw damaged("试题评分配置快照损坏");
        }
    }

    /** 执行 damaged 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException damaged(String message) {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, message);
    }
}
