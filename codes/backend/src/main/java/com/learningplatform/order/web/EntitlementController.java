package com.learningplatform.order.web;

import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.order.dto.EntitlementBalancesResponse;
import com.learningplatform.order.dto.EntitlementResponse;
import com.learningplatform.order.service.EntitlementService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entitlements")
public class EntitlementController {
    private final EntitlementService entitlementService;

    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping
    public ApiResponse<List<EntitlementResponse>> list(Authentication authentication) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(entitlementService.list(userId));
    }

    @GetMapping("/balances")
    public ApiResponse<EntitlementBalancesResponse> balances(Authentication authentication) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(entitlementService.balances(userId));
    }
}
