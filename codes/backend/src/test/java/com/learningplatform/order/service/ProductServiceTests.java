package com.learningplatform.order.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.order.domain.Product;
import com.learningplatform.order.domain.ProductStatus;
import com.learningplatform.order.domain.ProductType;
import com.learningplatform.order.dto.ProductResponse;
import com.learningplatform.order.dto.ProductWriteRequest;
import com.learningplatform.order.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTests {
    private ProductMapper productMapper;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        productService = new ProductService(productMapper);
        when(productMapper.findByCode(any())).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return 1;
        }).when(productMapper).insert(any(Product.class));
    }

    @Test
    void createsPaidContentProductWithResourceAndWithoutQuota() {
        ProductResponse response = productService.create(request(
                " content_database ",
                ProductType.CONTENT,
                100L,
                null
        ));

        assertThat(response.productCode()).isEqualTo("CONTENT_DATABASE");
        assertThat(response.resourceId()).isEqualTo(100L);
        assertThat(response.quantity()).isNull();
        assertThat(response.price()).isEqualByComparingTo("19.90");
    }

    @Test
    void createsAiPackageWithPositiveQuantityAndWithoutResource() {
        ProductResponse response = productService.create(request(
                "ai_package_10",
                ProductType.AI_PACKAGE,
                null,
                10
        ));

        assertThat(response.productType()).isEqualTo(ProductType.AI_PACKAGE);
        assertThat(response.resourceId()).isNull();
        assertThat(response.quantity()).isEqualTo(10);
    }

    @Test
    void createsExamPackageWithPositiveQuantityAndWithoutResource() {
        ProductResponse response = productService.create(request(
                "exam_package_5",
                ProductType.EXAM_PACKAGE,
                null,
                5
        ));

        assertThat(response.productType()).isEqualTo(ProductType.EXAM_PACKAGE);
        assertThat(response.quantity()).isEqualTo(5);
    }

    @Test
    void rejectsContentProductWithoutResourceOrWithQuota() {
        assertThatThrownBy(() -> productService.create(request(
                "CONTENT_INVALID",
                ProductType.CONTENT,
                null,
                null
        ))).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("付费资料商品必须关联有效资料");
        });

        assertThatThrownBy(() -> productService.create(request(
                "CONTENT_WITH_QUOTA",
                ProductType.CONTENT,
                100L,
                1
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getMessage()).isEqualTo("付费资料商品不能配置次数"));
    }

    @Test
    void rejectsQuotaPackageWithoutQuantityOrWithResource() {
        assertThatThrownBy(() -> productService.create(request(
                "AI_WITHOUT_QUOTA",
                ProductType.AI_PACKAGE,
                null,
                null
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getMessage()).isEqualTo("次数包商品必须配置大于0的次数"));

        assertThatThrownBy(() -> productService.create(request(
                "EXAM_WITH_RESOURCE",
                ProductType.EXAM_PACKAGE,
                100L,
                5
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getMessage()).isEqualTo("次数包商品不能关联资料"));
    }

    @Test
    void onlyReturnsActiveAndStructurallyValidProductAsPurchasable() {
        Product inactive = validPackage(ProductStatus.INACTIVE);
        when(productMapper.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> productService.getPurchasable(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("商品已下架");
                });

        Product active = validPackage(ProductStatus.ACTIVE);
        when(productMapper.findById(2L)).thenReturn(Optional.of(active));
        assertThat(productService.getPurchasable(2L)).isSameAs(active);
    }

    @Test
    void persistsNormalizedProductModel() {
        productService.create(request(
                "  exam_package_20  ",
                ProductType.EXAM_PACKAGE,
                null,
                20
        ));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductCode()).isEqualTo("EXAM_PACKAGE_20");
        assertThat(saved.getDescription()).isEqualTo("测试商品");
        assertThat(saved.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(saved.getSortOrder()).isZero();
    }

    private ProductWriteRequest request(
            String code,
            ProductType type,
            Long resourceId,
            Integer quantity
    ) {
        return new ProductWriteRequest(
                code,
                type,
                "测试商品",
                " 测试商品 ",
                resourceId,
                quantity,
                new BigDecimal("19.90"),
                null,
                null
        );
    }

    private Product validPackage(ProductStatus status) {
        Product product = new Product();
        product.setId(2L);
        product.setProductCode("AI_PACKAGE_10");
        product.setProductType(ProductType.AI_PACKAGE);
        product.setName("AI学习助手10次包");
        product.setQuantity(10);
        product.setPrice(new BigDecimal("9.90"));
        product.setStatus(status);
        product.setSortOrder(10);
        return product;
    }
}
