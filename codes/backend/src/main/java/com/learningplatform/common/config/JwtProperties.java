/* 文件职责：承载JWT配置属性配置属性，供配置装配和业务组件以类型安全方式读取。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：配置装配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
/**
 * 承载JWT配置属性配置属性，供配置装配和业务组件以类型安全方式读取。
 *
 * <p>职责边界：只负责组件装配和配置校验，不承载具体业务流程。</p>
 */
public record JwtProperties(String secret, Duration accessTokenTtl) {
    @Override
    /** 返回适合日志记录的文本表示；敏感 DTO 必须对密码、令牌或证件信息脱敏。 */
    public String toString() {
        return "JwtProperties[secret=[REDACTED], accessTokenTtl="
                + accessTokenTtl + "]";
    }
}
