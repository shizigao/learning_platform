/* 文件职责：定义更新学习进度请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 定义更新学习进度请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record UpdateLearningProgressRequest(
        @NotNull(message = "学习进度不能为空")
        @DecimalMin(value = "0.00", message = "学习进度不能小于0")
        @DecimalMax(value = "100.00", message = "学习进度不能超过100")
        BigDecimal progressPercent,

        @Size(max = 255, message = "学习位置不能超过255个字符")
        String lastPosition
) {
}
