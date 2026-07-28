/* 文件职责：承载Api频率限制配置属性配置属性，供配置装配和业务组件以类型安全方式读取。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：配置装配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rate-limit")
/**
 * 承载Api频率限制配置属性配置属性，供配置装配和业务组件以类型安全方式读取。
 *
 * <p>职责边界：只负责组件装配和配置校验，不承载具体业务流程。</p>
 */
public record ApiRateLimitProperties(
        boolean enabled,
        int generalRequestsPerMinute,
        int authRequestsPerMinute,
        int uploadRequestsPerMinute
) {
    /** 定义 DEFAULT_GENERAL_LIMIT 常量，统一该组件使用的固定规则或默认值。 */
    private static final int DEFAULT_GENERAL_LIMIT = 600;
    /** 定义 DEFAULT_AUTH_LIMIT 常量，统一该组件使用的固定规则或默认值。 */
    private static final int DEFAULT_AUTH_LIMIT = 30;
    /** 定义 DEFAULT_UPLOAD_LIMIT 常量，统一该组件使用的固定规则或默认值。 */
    private static final int DEFAULT_UPLOAD_LIMIT = 30;

    public ApiRateLimitProperties {
        generalRequestsPerMinute = positiveOrDefault(
                generalRequestsPerMinute,
                DEFAULT_GENERAL_LIMIT
        );
        authRequestsPerMinute = positiveOrDefault(
                authRequestsPerMinute,
                DEFAULT_AUTH_LIMIT
        );
        uploadRequestsPerMinute = positiveOrDefault(
                uploadRequestsPerMinute,
                DEFAULT_UPLOAD_LIMIT
        );
    }

    /** 执行 positiveOrDefault 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private static int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
