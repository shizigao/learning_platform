/* 文件职责：定义Join班级请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义Join班级请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record JoinClassRequest(
        @NotBlank(message = "班级邀请码不能为空")
        @Size(max = 32, message = "班级邀请码格式错误")
        String inviteCode
) {
}
