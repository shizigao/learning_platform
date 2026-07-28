/* 文件职责：定义考试答案Batch保存请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 定义考试答案Batch保存请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamAnswerBatchSaveRequest(
        @NotEmpty(message = "批量保存答案不能为空")
        @Size(max = 200, message = "单次最多保存200道题")
        List<@Valid ExamAnswerBatchItemRequest> answers
) {
    public ExamAnswerBatchSaveRequest {
        answers = answers == null ? List.of() : List.copyOf(answers);
    }
}
