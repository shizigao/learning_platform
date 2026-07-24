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
public class ExamAnswerPresentationService {
    private final ObjectMapper objectMapper;

    public ExamAnswerPresentationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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

    public boolean fillBlankAutoGradable(ExamAnswer answer) {
        return snapshotBoolean(
                answer.getAnswerSnapshot(),
                "fillBlankAutoGradable",
                Boolean.TRUE.equals(answer.getFillBlankAutoGradable())
        );
    }

    public boolean caseSensitive(ExamAnswer answer) {
        return snapshotBoolean(
                answer.getAnswerSnapshot(),
                "caseSensitive",
                Boolean.TRUE.equals(answer.getCaseSensitive())
        );
    }

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

    private boolean snapshotBoolean(String snapshot, String field, boolean fallback) {
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            return root.has(field) ? root.path(field).asBoolean() : fallback;
        } catch (JsonProcessingException exception) {
            throw damaged("试题评分配置快照损坏");
        }
    }

    private BusinessException damaged(String message) {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, message);
    }
}
