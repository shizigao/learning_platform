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

public final class OfflineTeachingDtos {
    private OfflineTeachingDtos() {
    }

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

    public record ReviewRequest(
            @Size(max = 1000) String reason
    ) {
    }

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

    public record RecommendationGenerateRequest(
            @NotBlank @Size(max = 64) String requestId,
            @Valid StudentPreferenceRequest preference
    ) {
    }

    public record TeacherRecommendationItem(
            TeacherProfileResponse teacher,
            String reason,
            List<String> matchHighlights,
            int localScore
    ) {
    }

    public record TeacherRecommendationResponse(
            boolean aiSucceeded,
            String message,
            AiTaskResponse task,
            List<TeacherRecommendationItem> recommendations,
            LocalDateTime createdAt
    ) {
    }
}
