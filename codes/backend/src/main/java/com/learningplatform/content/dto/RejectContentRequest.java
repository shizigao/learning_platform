/* 文件职责：定义驳回学习资料请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义驳回学习资料请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record RejectContentRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 1000, message = "驳回原因不能超过1000个字符")
        String reason
) {
}
