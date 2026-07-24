package com.learningplatform.exam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExamWriteRequest(
        @NotNull(message = "试卷ID不能为空")
        @Min(value = 1, message = "试卷ID必须为正数")
        Long paperId,

        @NotBlank(message = "考试名称不能为空")
        @Size(max = 200, message = "考试名称不能超过200个字符")
        String name,

        @Size(max = 5000, message = "考试说明不能超过5000个字符")
        String instructions,

        @NotNull(message = "考试开始时间不能为空")
        LocalDateTime startAt,

        @NotNull(message = "考试结束时间不能为空")
        LocalDateTime endAt,

        @NotNull(message = "答题时长不能为空")
        @Min(value = 1, message = "答题时长至少为1分钟")
        @Max(value = 10080, message = "答题时长不能超过7天")
        Integer durationMinutes,

        @NotNull(message = "及格分不能为空")
        @DecimalMin(value = "0.00", message = "及格分不能小于0")
        @DecimalMax(value = "999999.99", message = "及格分不能超过999999.99")
        BigDecimal passingScore,

        Boolean showResultImmediately,

        Boolean showAnswerAfterFinish,

        @Size(max = 1000, message = "单场考试最多指定1000名考生")
        List<@NotNull(message = "考生ID不能为空") @Min(value = 1, message = "考生ID必须为正数") Long> candidateUserIds
) {
}
