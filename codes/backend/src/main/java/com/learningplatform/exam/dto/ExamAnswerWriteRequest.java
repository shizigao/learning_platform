/* 文件职责：定义考试答案Write请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 定义考试答案Write请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamAnswerWriteRequest(
        @Size(max = 50, message = "结构化答案数量不能超过50项")
        List<@Size(max = 2000, message = "单项答案不能超过2000个字符") String> values,

        @Size(max = 20000, message = "文本答案不能超过20000个字符")
        String text
) {
    public ExamAnswerWriteRequest {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
