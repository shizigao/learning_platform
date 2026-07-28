/* 文件职责：以不可变记录表示题目答案数据，并作为模块内部或接口层的数据契约。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
