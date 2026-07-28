/* 文件职责：提供管理AI相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.web;

import com.learningplatform.ai.dto.AdminAiConfigResponse;
import com.learningplatform.ai.service.AdminAiConfigService;
import com.learningplatform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
/**
 * 提供管理AI相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class AdminAiController {
    /** 委托配置执行对应领域规则。 */
    private final AdminAiConfigService configService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AdminAiController(AdminAiConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/config")
    /** 处理 GET /config 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AdminAiConfigResponse> config() {
        return ApiResponse.success(configService.current());
    }
}
