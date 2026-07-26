package com.learningplatform.content.dto;

import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.domain.ContentDistributionMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ContentWriteRequest(
        @NotNull(message = "资料分类不能为空")
        @Min(value = 1, message = "资料分类ID必须为正数")
        Long categoryId,

        @NotBlank(message = "资料标题不能为空")
        @Size(max = 200, message = "资料标题不能超过200个字符")
        String title,

        @Size(max = 1000, message = "资料简介不能超过1000个字符")
        String summary,

        // 兼容旧客户端传参；服务端统一存储为 GENERAL，不再要求用户选择资料类型。
        ContentType contentType,

        String articleBody,

        ContentDistributionMode distributionMode,

        @Size(max = 50, message = "单份资料最多发放到50个班级")
        List<@NotNull(message = "班级ID不能为空") @Min(value = 1, message = "班级ID必须为正数") Long> classIds,

        @NotNull(message = "必须明确资料是否免费")
        Boolean isFree,

        @DecimalMin(value = "0.00", message = "价格不能为负数")
        BigDecimal price
) {
    public ContentWriteRequest(
            Long categoryId,
            String title,
            String summary,
            ContentType contentType,
            String articleBody,
            Boolean isFree,
            BigDecimal price
    ) {
        this(
                categoryId,
                title,
                summary,
                contentType,
                articleBody,
                ContentDistributionMode.PUBLIC,
                List.of(),
                isFree,
                price
        );
    }
}
