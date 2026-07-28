/* 文件职责：定义创建评论请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义创建评论请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record CreateCommentRequest(
        @Min(value = 1, message = "父评论ID必须为正数")
        Long parentId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 2000, message = "评论内容不能超过2000个字符")
        String body
) {
}
