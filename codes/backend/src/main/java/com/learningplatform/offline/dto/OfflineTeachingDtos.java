/* 文件职责：表示线下教学教学Dtos领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.dto;

import com.learningplatform.ai.dto.AiTaskResponse;
import com.learningplatform.offline.domain.TeacherApplicationStatus;
import com.learningplatform.offline.domain.TeacherProfileStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 表示线下教学教学Dtos领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public final class OfflineTeachingDtos {
    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    private OfflineTeachingDtos() {
    }

    /** 执行 TeacherApplicationRequest 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record TeacherApplicationRequest(
            @NotBlank @Size(max = 64) String teacherName,
            @NotBlank
            @Pattern(
                    regexp = "(^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])"
                            + "([0-2][1-9]|10|20|30|31)\\d{3}[0-9Xx]$)"
                            + "|(^[1-9]\\d{7}(0[1-9]|1[0-2])"
                            + "([0-2][1-9]|10|20|30|31)\\d{3}$)",
                    message = "身份证号格式不正确"
            )
            String idCardNumber,
            @NotBlank @Pattern(regexp = "UNKNOWN|MALE|FEMALE") String gender,
            @NotBlank
            @Pattern(regexp = "HIGH_SCHOOL|ASSOCIATE|BACHELOR|MASTER|DOCTOR|OTHER")
            String educationLevel,
            @NotBlank @Size(max = 1000) String educationBackground,
            @Size(max = 200) String institution,
            @NotBlank @Size(max = 100) String province,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 100) String district,
            @NotBlank @Size(max = 2000) String bio,
            @NotBlank @Size(max = 2000) String teachingContent,
            @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 50) String> teachingTags,
            @NotBlank @Size(max = 1000) String availability,
            @NotNull @DecimalMin(value = "0.01") BigDecimal hourlyRate,
            @Size(max = 500) String priceDescription,
            @Size(max = 100) String contactWechat,
            @Pattern(regexp = "^$|^[1-9][0-9]{4,11}$", message = "QQ号格式不正确")
            String contactQq,
            @Email @Size(max = 128) String contactEmail
    ) {
    }

    /** 执行 TeacherApplicationResponse 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record TeacherApplicationResponse(
            Long id,
            Long userId,
            String teacherName,
            String idCardMasked,
            String idCardNumber,
            String gender,
            String educationLevel,
            String educationBackground,
            String institution,
            String province,
            String city,
            String district,
            String bio,
            String teachingContent,
            List<String> teachingTags,
            String availability,
            BigDecimal hourlyRate,
            String priceDescription,
            String contactWechat,
            String contactQq,
            String contactEmail,
            TeacherApplicationStatus status,
            String rejectionReason,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            LocalDateTime updatedAt
    ) {
    }

    /** 执行 TeacherApplicationSummary 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record TeacherApplicationSummary(
            Long id,
            Long userId,
            String username,
            String nickname,
            String teacherName,
            String idCardMasked,
            String province,
            String city,
            String institution,
            TeacherApplicationStatus status,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            LocalDateTime updatedAt
    ) {
    }

    /** 执行 TeacherProfileResponse 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record TeacherProfileResponse(
            Long id,
            Long userId,
            String username,
            String nickname,
            String avatarUrl,
            String teacherName,
            String gender,
            String educationLevel,
            String educationBackground,
            String institution,
            String province,
            String city,
            String district,
            String bio,
            String teachingContent,
            List<String> teachingTags,
            String availability,
            BigDecimal hourlyRate,
            String priceDescription,
            String contactWechat,
            String contactQq,
            String contactEmail,
            TeacherProfileStatus status,
            String suspendedReason,
            LocalDateTime approvedAt
    ) {
    }

    /** 执行 ReviewRequest 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record ReviewRequest(
            @Size(max = 1000) String reason
    ) {
    }

    /** 执行 StudentPreferenceRequest 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record StudentPreferenceRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 500) String currentLevel,
            @NotBlank @Size(max = 2000) String learningGoals,
            @Size(max = 2000) String weaknesses,
            @NotBlank @Size(max = 100) String province,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 100) String district,
            @DecimalMin(value = "0.01") BigDecimal maxHourlyRate,
            @Size(max = 1000) String availability,
            @Size(max = 2000) String teacherPreferences,
            @Size(max = 2000) String additionalNotes
    ) {
    }

    /** 执行推荐生成请求核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    public record RecommendationGenerateRequest(
            @NotBlank @Size(max = 64) String requestId,
            @Valid StudentPreferenceRequest preference
    ) {
    }

    /** 执行 TeacherRecommendationItem 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record TeacherRecommendationItem(
            TeacherProfileResponse teacher,
            String reason,
            List<String> matchHighlights,
            int localScore
    ) {
    }

    /** 执行 TeacherRecommendationResponse 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record TeacherRecommendationResponse(
            boolean aiSucceeded,
            String message,
            AiTaskResponse task,
            List<TeacherRecommendationItem> recommendations,
            LocalDateTime createdAt
    ) {
    }
}
