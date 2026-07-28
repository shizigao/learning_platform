/* 文件职责：定义替换试卷Questions请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 定义替换试卷Questions请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ReplacePaperQuestionsRequest(
        @NotEmpty(message = "试卷至少需要一道题")
        @Size(max = 500, message = "单份试卷不能超过500道题")
        List<@NotNull(message = "试卷题目不能为空") @Valid PaperQuestionWriteRequest> questions
) {
}
