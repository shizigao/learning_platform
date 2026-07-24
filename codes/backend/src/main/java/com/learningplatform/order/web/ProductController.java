package com.learningplatform.order.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.order.domain.ProductType;
import com.learningplatform.order.dto.ProductResponse;
import com.learningplatform.order.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> list(
            @RequestParam(required = false) ProductType productType
    ) {
        return ApiResponse.success(productService.listActive(productType));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long productId) {
        return ApiResponse.success(ProductResponse.from(
                productService.getPurchasable(productId)
        ));
    }
}
