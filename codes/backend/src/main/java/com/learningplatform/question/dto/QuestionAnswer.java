package com.learningplatform.question.dto;

import java.util.List;

/**
 * 所有题型的统一答案结构。
 * 外层按“答案位置”排序，内层是该位置可接受的值。
 */
public record QuestionAnswer(List<List<String>> acceptedAnswers) {
    public QuestionAnswer {
        acceptedAnswers = acceptedAnswers == null
                ? List.of()
                : acceptedAnswers.stream().map(List::copyOf).toList();
    }
}
