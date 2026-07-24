package com.learningplatform.ai.web;

import com.learningplatform.ai.dto.AdminAiConfigResponse;
import com.learningplatform.ai.service.AdminAiConfigService;
import com.learningplatform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {
    private final AdminAiConfigService configService;

    public AdminAiController(AdminAiConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/config")
    public ApiResponse<AdminAiConfigResponse> config() {
        return ApiResponse.success(configService.current());
    }
}
