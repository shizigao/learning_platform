/* 文件职责：表示学习Platform申请领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：应用启动与领域模块装配；所在分层：模块根目录。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableScheduling
/**
 * 表示学习Platform申请领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：遵守 应用启动与领域模块装配 模块的职责边界。</p>
 */
public class LearningPlatformApplication {

    /** 执行 main 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public static void main(String[] args) {
        SpringApplication.run(LearningPlatformApplication.class, args);
    }
}
